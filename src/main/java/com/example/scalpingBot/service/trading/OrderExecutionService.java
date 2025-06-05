package com.example.scalpingBot.service.trading;

import com.example.scalpingBot.entity.Trade;
import com.example.scalpingBot.enums.OrderStatus;
import com.example.scalpingBot.enums.OrderType;
import com.example.scalpingBot.exception.ExchangeApiException;
import com.example.scalpingBot.exception.TradingException;
import com.example.scalpingBot.repository.TradeRepository;
import com.example.scalpingBot.service.exchange.ExchangeApiService;
import com.example.scalpingBot.utils.DateUtils;
import com.example.scalpingBot.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис для исполнения торговых ордеров на биржах
 *
 * Основные функции:
 * - Размещение ордеров на биржах (Binance, Bybit)
 * - Мониторинг статуса исполнения в реальном времени
 * - Автоматическая отмена просроченных ордеров
 * - Retry логика для обработки временных сбоев
 * - Расчет slippage и комиссий
 * - Обновление статусов в базе данных
 * - Уведомления об изменениях статуса
 *
 * Все операции оптимизированы для скальпинг-стратегии
 * с минимальными задержками исполнения.
 *
 * @author ScalpingBot Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderExecutionService {

    private final TradeRepository tradeRepository;
    private final ExchangeApiService exchangeApiService;
    private final NotificationService notificationService;

    /**
     * Кеш активных ордеров для быстрого мониторинга
     */
    private final Map<String, Trade> activeOrdersCache = new ConcurrentHashMap<>();

    /**
     * Счетчик попыток исполнения для каждого ордера
     */
    private final Map<Long, Integer> retryCounters = new ConcurrentHashMap<>();

    /**
     * Константы для исполнения ордеров
     */
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int RETRY_DELAY_MS = 1000;
    private static final int ORDER_TIMEOUT_SECONDS = 30;
    private static final BigDecimal MAX_SLIPPAGE_PERCENT = new BigDecimal("0.5");

    /**
     * Исполнить ордер асинхронно
     *
     * @param trade торговая операция для исполнения
     * @return CompletableFuture с результатом исполнения
     */
    @Async
    @Retryable(value = {ExchangeApiException.class}, maxAttempts = MAX_RETRY_ATTEMPTS,
            backoff = @Backoff(delay = RETRY_DELAY_MS))
    public CompletableFuture<Trade> executeOrder(Trade trade) {
        try {
            log.info("Executing order: {} {} {} at {}",
                    trade.getOrderSide(), trade.getQuantity(), trade.getTradingPair(),
                    trade.getPrice() != null ? trade.getPrice() : "MARKET");

            // Валидируем ордер перед исполнением
            validateOrder(trade);

            // Обновляем статус на "отправлен"
            updateOrderStatus(trade, OrderStatus.SUBMITTED);

            // Добавляем в кеш для мониторинга
            activeOrdersCache.put(trade.getExchangeOrderId(), trade);

            // Исполняем ордер на бирже
            Trade executedTrade = executeOrderOnExchange(trade);

            // Обрабатываем результат исполнения
            return CompletableFuture.completedFuture(processExecutionResult(executedTrade));

        } catch (Exception e) {
            log.error("Failed to execute order {}: {}", trade.getId(), e.getMessage());
            return handleOrderExecutionError(trade, e);
        }
    }

    /**
     * Валидировать ордер перед исполнением
     *
     * @param trade торговая операция
     * @throws TradingException если валидация не пройдена
     */
    private void validateOrder(Trade trade) {
        try {
            // Базовая валидация параметров
            ValidationUtils.validateOrderParameters(
                    trade.getTradingPair(),
                    trade.getOrderType(),
                    trade.getOrderSide(),
                    trade.getQuantity(),
                    trade.getPrice()
            );

            // Проверяем минимальный объем
            if (trade.getPrice() != null) {
                BigDecimal notional = trade.getQuantity().multiply(trade.getPrice());
                ValidationUtils.validateUsdtVolume(notional);
            }

            // Проверяем, что ордер еще актуален (не слишком старый)
            if (DateUtils.hasElapsedSeconds(trade.getCreatedAt(), ORDER_TIMEOUT_SECONDS)) {
                throw TradingException.orderRejected(trade.getTradingPair(),
                        "Order timeout exceeded", OrderStatus.EXPIRED);
            }

        } catch (Exception e) {
            throw new TradingException(TradingException.TradingErrorType.INVALID_POSITION_SIZE,
                    "Order validation failed: " + e.getMessage(), trade.getTradingPair(), e);
        }
    }

    /**
     * Исполнить ордер на бирже
     *
     * @param trade торговая операция
     * @return обновленная торговая операция
     * @throws ExchangeApiException если ошибка API биржи
     */
    private Trade executeOrderOnExchange(Trade trade) throws ExchangeApiException {
        try {
            Map<String, Object> orderParams = buildOrderParameters(trade);
            Map<String, Object> result;

            // Размещаем ордер в зависимости от типа
            switch (trade.getOrderType()) {
                case MARKET:
                    result = exchangeApiService.placeMarketOrder(
                            trade.getTradingPair(),
                            trade.getOrderSide().getCode(),
                            trade.getQuantity(),
                            trade.getExchangeName()
                    );
                    break;

                case LIMIT:
                    result = exchangeApiService.placeLimitOrder(
                            trade.getTradingPair(),
                            trade.getOrderSide().getCode(),
                            trade.getQuantity(),
                            trade.getPrice(),
                            trade.getExchangeName()
                    );
                    break;

                case STOP_LOSS:
                    result = exchangeApiService.placeStopOrder(
                            trade.getTradingPair(),
                            trade.getOrderSide().getCode(),
                            trade.getQuantity(),
                            trade.getStopLossPrice(),
                            trade.getExchangeName()
                    );
                    break;

                default:
                    throw new TradingException(TradingException.TradingErrorType.INVALID_POSITION_SIZE,
                            "Unsupported order type: " + trade.getOrderType());
            }

            // Обновляем торговую операцию данными от биржи
            return updateTradeFromExchangeResponse(trade, result);

        } catch (ExchangeApiException e) {
            log.error("Exchange API error for order {}: {}", trade.getId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error executing order {}: {}", trade.getId(), e.getMessage());
            throw new ExchangeApiException("binance", ExchangeApiException.ApiErrorType.UNKNOWN_ERROR,
                    "Unexpected execution error: " + e.getMessage(), e);
        }
    }

    /**
     * Построить параметры ордера для биржи
     *
     * @param trade торговая операция
     * @return параметры ордера
     */
    private Map<String, Object> buildOrderParameters(Trade trade) {
        Map<String, Object> params = new ConcurrentHashMap<>();

        params.put("symbol", trade.getTradingPair());
        params.put("side", trade.getOrderSide().getCode());
        params.put("type", trade.getOrderType().getCode());
        params.put("quantity", trade.getQuantity().toString());

        if (trade.getPrice() != null) {
            params.put("price", trade.getPrice().toString());
        }

        if (trade.getStopLossPrice() != null) {
            params.put("stopPrice", trade.getStopLossPrice().toString());
        }

        // Добавляем клиентский ID для отслеживания
        if (trade.getClientOrderId() == null) {
            trade.setClientOrderId("SCALP_" + System.currentTimeMillis());
        }
        params.put("newClientOrderId", trade.getClientOrderId());

        // Для рыночных ордеров устанавливаем timeInForce
        if (trade.getOrderType() == OrderType.MARKET) {
            params.put("timeInForce", "IOC"); // Immediate or Cancel
        } else {
            params.put("timeInForce", "GTC"); // Good Till Cancelled
        }

        return params;
    }

    /**
     * Обновить торговую операцию ответом от биржи
     *
     * @param trade торговая операция
     * @param exchangeResponse ответ биржи
     * @return обновленная торговая операция
     */
    private Trade updateTradeFromExchangeResponse(Trade trade, Map<String, Object> exchangeResponse) {
        try {
            // Обновляем основные поля
            trade.setExchangeOrderId(exchangeResponse.get("orderId").toString());
            trade.setExchangeTimestamp((Long) exchangeResponse.get("transactTime"));

            // Обновляем статус
            String exchangeStatus = exchangeResponse.get("status").toString();
            OrderStatus newStatus = mapExchangeStatusToOrderStatus(exchangeStatus);
            trade.setStatus(newStatus);

            // Обновляем исполненные данные
            if (exchangeResponse.containsKey("executedQty")) {
                BigDecimal executedQty = new BigDecimal(exchangeResponse.get("executedQty").toString());
                trade.setExecutedQuantity(executedQty);
            }

            if (exchangeResponse.containsKey("cummulativeQuoteQty")) {
                BigDecimal cummulativeQuoteQty = new BigDecimal(exchangeResponse.get("cummulativeQuoteQty").toString());
                trade.setTotalValue(cummulativeQuoteQty);

                // Рассчитываем среднюю цену
                if (trade.getExecutedQuantity() != null && trade.getExecutedQuantity().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal avgPrice = cummulativeQuoteQty.divide(trade.getExecutedQuantity(), 8, BigDecimal.ROUND_HALF_UP);
                    trade.setAvgPrice(avgPrice);
                }
            }

            // Рассчитываем slippage для рыночных ордеров
            if (trade.getOrderType() == OrderType.MARKET && trade.getPrice() != null && trade.getAvgPrice() != null) {
                BigDecimal slippage = calculateSlippage(trade.getPrice(), trade.getAvgPrice(), trade.getOrderSide());
                trade.setSlippagePercent(slippage);
            }

            // Обновляем временные метки
            if (newStatus == OrderStatus.FILLED || newStatus == OrderStatus.PARTIALLY_FILLED) {
                trade.setExecutedAt(DateUtils.nowMoscow());
            }

            return trade;

        } catch (Exception e) {
            log.error("Failed to update trade from exchange response: {}", e.getMessage());
            throw new RuntimeException("Failed to process exchange response", e);
        }
    }

    /**
     * Рассчитать slippage
     *
     * @param expectedPrice ожидаемая цена
     * @param actualPrice фактическая цена
     * @param orderSide сторона ордера
     * @return slippage в процентах
     */
    private BigDecimal calculateSlippage(BigDecimal expectedPrice, BigDecimal actualPrice, com.example.scalpingBot.enums.OrderSide orderSide) {
        BigDecimal priceDiff = actualPrice.subtract(expectedPrice);

        // Для покупки отрицательный slippage = хуже ожидаемого
        // Для продажи положительный slippage = хуже ожидаемого
        if (orderSide == com.example.scalpingBot.enums.OrderSide.SELL) {
            priceDiff = priceDiff.negate();
        }

        return priceDiff.divide(expectedPrice, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    /**
     * Преобразовать статус биржи в внутренний статус
     *
     * @param exchangeStatus статус от биржи
     * @return внутренний статус
     */
    private OrderStatus mapExchangeStatusToOrderStatus(String exchangeStatus) {
        switch (exchangeStatus.toUpperCase()) {
            case "NEW":
                return OrderStatus.SUBMITTED;
            case "PARTIALLY_FILLED":
                return OrderStatus.PARTIALLY_FILLED;
            case "FILLED":
                return OrderStatus.FILLED;
            case "CANCELED":
            case "CANCELLED":
                return OrderStatus.CANCELLED;
            case "PENDING_CANCEL":
                return OrderStatus.SUBMITTED; // Все еще активен
            case "REJECTED":
                return OrderStatus.REJECTED;
            case "EXPIRED":
                return OrderStatus.EXPIRED;
            default:
                log.warn("Unknown exchange status: {}", exchangeStatus);
                return OrderStatus.FAILED;
        }
    }

    /**
     * Обработать результат исполнения ордера
     *
     * @param trade исполненная торговая операция
     * @return финальная торговая операция
     */
    private Trade processExecutionResult(Trade trade) {
        try {
            // Сохраняем в БД
            trade = tradeRepository.save(trade);

            // Удаляем счетчик попыток
            retryCounters.remove(trade.getId());

            // Логируем результат
            logExecutionResult(trade);

            // Отправляем уведомления
            sendExecutionNotification(trade);

            // Удаляем из кеша если ордер завершен
            if (trade.isCompleted()) {
                activeOrdersCache.remove(trade.getExchangeOrderId());
            }

            return trade;

        } catch (Exception e) {
            log.error("Failed to process execution result for order {}: {}", trade.getId(), e.getMessage());
            throw new RuntimeException("Failed to process execution result", e);
        }
    }

    /**
     * Логировать результат исполнения
     *
     * @param trade торговая операция
     */
    private void logExecutionResult(Trade trade) {
        String statusEmoji = getStatusEmoji(trade.getStatus());

        if (trade.getStatus() == OrderStatus.FILLED) {
            log.info("{} Order FILLED: {} {} {} at avg price {} (slippage: {}%)",
                    statusEmoji, trade.getOrderSide(), trade.getExecutedQuantity(),
                    trade.getTradingPair(), trade.getAvgPrice(),
                    trade.getSlippagePercent() != null ? trade.getSlippagePercent() : "N/A");
        } else if (trade.getStatus() == OrderStatus.PARTIALLY_FILLED) {
            log.info("{} Order PARTIALLY FILLED: {} of {} {} at avg price {}",
                    statusEmoji, trade.getExecutedQuantity(), trade.getQuantity(),
                    trade.getTradingPair(), trade.getAvgPrice());
        } else if (trade.getStatus() == OrderStatus.REJECTED) {
            log.warn("{} Order REJECTED: {} {} {} - Check parameters",
                    statusEmoji, trade.getOrderSide(), trade.getQuantity(), trade.getTradingPair());
        } else {
            log.info("{} Order status: {} for {} {} {}",
                    statusEmoji, trade.getStatus(), trade.getOrderSide(),
                    trade.getQuantity(), trade.getTradingPair());
        }
    }

    /**
     * Получить эмодзи для статуса
     *
     * @param status статус ордера
     * @return эмодзи
     */
    private String getStatusEmoji(OrderStatus status) {
        return status.getEmoji();
    }

    /**
     * Отправить уведомление об исполнении
     *
     * @param trade торговая операция
     */
    private void sendExecutionNotification(Trade trade) {
        try {
            if (trade.getStatus() == OrderStatus.FILLED) {
                String message = String.format(
                        "Order Executed:\n" +
                                "%s %s %s\n" +
                                "Price: %s\n" +
                                "Value: %s USDT\n" +
                                "Slippage: %s%%",
                        trade.getOrderSide(),
                        trade.getExecutedQuantity(),
                        trade.getTradingPair(),
                        trade.getAvgPrice(),
                        trade.getTotalValue(),
                        trade.getSlippagePercent() != null ? trade.getSlippagePercent() : "0"
                );

                notificationService.sendTradeAlert("Order Filled", message);

            } else if (trade.getStatus() == OrderStatus.REJECTED) {
                String message = String.format(
                        "Order Rejected:\n" +
                                "%s %s %s at %s\n" +
                                "Check account balance and parameters",
                        trade.getOrderSide(),
                        trade.getQuantity(),
                        trade.getTradingPair(),
                        trade.getPrice()
                );

                notificationService.sendErrorAlert("Order Rejected", message);
            }

        } catch (Exception e) {
            log.error("Failed to send execution notification: {}", e.getMessage());
        }
    }

    /**
     * Обработать ошибку исполнения ордера
     *
     * @param trade торговая операция
     * @param error ошибка
     * @return CompletableFuture с обработанной ошибкой
     */
    private CompletableFuture<Trade> handleOrderExecutionError(Trade trade, Throwable error) {
        try {
            // Увеличиваем счетчик попыток
            int attempts = retryCounters.merge(trade.getId(), 1, Integer::sum);

            log.error("Order execution error (attempt {}): {}", attempts, error.getMessage());

            // Определяем финальный статус
            OrderStatus finalStatus;
            if (error instanceof ExchangeApiException) {
                ExchangeApiException apiError = (ExchangeApiException) error;
                if (apiError.isRetryable() && attempts < MAX_RETRY_ATTEMPTS) {
                    // Планируем повторную попытку
                    return retryOrderExecution(trade, apiError);
                } else {
                    finalStatus = apiError.isAuthenticationError() ? OrderStatus.REJECTED : OrderStatus.FAILED;
                }
            } else {
                finalStatus = OrderStatus.FAILED;
            }

            // Обновляем статус ордера
            updateOrderStatus(trade, finalStatus);
            trade.setNotes(error.getMessage());

            // Удаляем из активных ордеров
            activeOrdersCache.remove(trade.getExchangeOrderId());
            retryCounters.remove(trade.getId());

            // Сохраняем в БД
            trade = tradeRepository.save(trade);

            // Уведомляем об ошибке
            notificationService.sendErrorAlert("Order Execution Failed",
                    String.format("Failed to execute %s %s %s: %s",
                            trade.getOrderSide(), trade.getQuantity(), trade.getTradingPair(), error.getMessage()));

            return CompletableFuture.completedFuture(trade);

        } catch (Exception e) {
            log.error("Failed to handle order execution error: {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Повторить исполнение ордера
     *
     * @param trade торговая операция
     * @param error предыдущая ошибка
     * @return CompletableFuture с результатом повтора
     */
    private CompletableFuture<Trade> retryOrderExecution(Trade trade, ExchangeApiException error) {
        long retryDelay = error.getRetryDelayMs();

        log.info("Retrying order execution for {} in {} ms", trade.getId(), retryDelay);

        // Возвращаем CompletableFuture который выполнится после задержки
        return CompletableFuture
                .delayedExecutor(retryDelay, java.util.concurrent.TimeUnit.MILLISECONDS)
                .execute(() -> executeOrder(trade));
    }

    /**
     * Отменить ордер
     *
     * @param trade торговая операция для отмены
     * @return CompletableFuture с результатом отмены
     */
    @Async
    public CompletableFuture<Trade> cancelOrder(Trade trade) {
        try {
            log.info("Cancelling order: {} {}", trade.getId(), trade.getExchangeOrderId());

            if (trade.getExchangeOrderId() == null) {
                // Ордер еще не был отправлен на биржу
                updateOrderStatus(trade, OrderStatus.CANCELLED);
                trade.setCancelledAt(DateUtils.nowMoscow());
                trade = tradeRepository.save(trade);
                return CompletableFuture.completedFuture(trade);
            }

            // Отменяем на бирже
            Map<String, Object> result = exchangeApiService.cancelOrder(
                    trade.getTradingPair(),
                    trade.getExchangeOrderId(),
                    trade.getExchangeName()
            );

            // Обновляем статус
            String exchangeStatus = result.get("status").toString();
            OrderStatus newStatus = mapExchangeStatusToOrderStatus(exchangeStatus);

            updateOrderStatus(trade, newStatus);
            trade.setCancelledAt(DateUtils.nowMoscow());

            // Удаляем из активных ордеров
            activeOrdersCache.remove(trade.getExchangeOrderId());

            // Сохраняем
            trade = tradeRepository.save(trade);

            log.info("Order cancelled successfully: {}", trade.getId());
            return CompletableFuture.completedFuture(trade);

        } catch (Exception e) {
            log.error("Failed to cancel order {}: {}", trade.getId(), e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Обновить статус ордера
     *
     * @param trade торговая операция
     * @param newStatus новый статус
     */
    private void updateOrderStatus(Trade trade, OrderStatus newStatus) {
        OrderStatus oldStatus = trade.getStatus();
        trade.setStatus(newStatus);
        trade.setUpdatedAt(DateUtils.nowMoscow());

        log.debug("Order {} status changed: {} → {}", trade.getId(), oldStatus, newStatus);
    }

    /**
     * Мониторинг активных ордеров - выполняется каждые 10 секунд
     */
    @Scheduled(fixedRate = 10000)
    public void monitorActiveOrders() {
        try {
            if (activeOrdersCache.isEmpty()) {
                return;
            }

            log.debug("Monitoring {} active orders", activeOrdersCache.size());

            for (Trade trade : activeOrdersCache.values()) {
                try {
                    monitorSingleOrder(trade);
                } catch (Exception e) {
                    log.error("Failed to monitor order {}: {}", trade.getId(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error in order monitoring: {}", e.getMessage());
        }
    }

    /**
     * Мониторить один ордер
     *
     * @param trade торговая операция
     */
    private void monitorSingleOrder(Trade trade) {
        try {
            // Проверяем таймаут
            if (DateUtils.hasElapsedSeconds(trade.getCreatedAt(), ORDER_TIMEOUT_SECONDS)) {
                log.warn("Order {} timed out, cancelling", trade.getId());
                cancelOrder(trade);
                return;
            }

            // Запрашиваем статус у биржи
            if (trade.getExchangeOrderId() != null) {
                Map<String, Object> orderStatus = exchangeApiService.getOrderStatus(
                        trade.getTradingPair(),
                        trade.getExchangeOrderId(),
                        trade.getExchangeName()
                );

                // Обновляем данные ордера
                Trade updatedTrade = updateTradeFromExchangeResponse(trade, orderStatus);

                if (updatedTrade.isCompleted()) {
                    processExecutionResult(updatedTrade);
                }
            }

        } catch (Exception e) {
            log.error("Failed to monitor order {}: {}", trade.getId(), e.getMessage());
        }
    }

    /**
     * Очистка просроченных ордеров - выполняется каждую минуту
     */
    @Scheduled(fixedRate = 60000)
    public void cleanupExpiredOrders() {
        try {
            LocalDateTime threshold = DateUtils.nowMoscow().minusMinutes(5);
            List<Trade> expiredOrders = tradeRepository.findExpiredOrders(threshold);

            if (!expiredOrders.isEmpty()) {
                log.info("Found {} expired orders for cleanup", expiredOrders.size());

                for (Trade order : expiredOrders) {
                    try {
                        if (order.isActive()) {
                            cancelOrder(order);
                        }
                    } catch (Exception e) {
                        log.error("Failed to cleanup expired order {}: {}", order.getId(), e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error in expired orders cleanup: {}", e.getMessage());
        }
    }

    /**
     * Получить статистику исполнения ордеров
     *
     * @return статистика ордеров
     */
    public OrderExecutionStatistics getExecutionStatistics() {
        try {
            long totalOrders = tradeRepository.count();
            long filledOrders = tradeRepository.countByStatus(OrderStatus.FILLED);
            long rejectedOrders = tradeRepository.countByStatus(OrderStatus.REJECTED);
            long activeOrders = activeOrdersCache.size();

            // Средний slippage
            List<Trade> recentTrades = tradeRepository.findTradesSince(DateUtils.nowMoscow().minusHours(24));
            BigDecimal avgSlippage = recentTrades.stream()
                    .filter(t -> t.getSlippagePercent() != null)
                    .map(Trade::getSlippagePercent)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(new BigDecimal(Math.max(1, recentTrades.size())), 4, BigDecimal.ROUND_HALF_UP);

            double fillRate = totalOrders > 0 ? (double) filledOrders / totalOrders * 100 : 0;

            return OrderExecutionStatistics.builder()
                    .totalOrders((int) totalOrders)
                    .filledOrders((int) filledOrders)
                    .rejectedOrders((int) rejectedOrders)
                    .activeOrders((int) activeOrders)
                    .fillRate(fillRate)
                    .averageSlippage(avgSlippage)
                    .build();

        } catch (Exception e) {
            log.error("Failed to get execution statistics: {}", e.getMessage());
            return OrderExecutionStatistics.builder().build();
        }
    }

    // === Вложенные классы ===

    /**
     * Статистика исполнения ордеров
     */
    @lombok.Data
    @lombok.Builder
    public static class OrderExecutionStatistics {
        private int totalOrders;
        private int filledOrders;
        private int rejectedOrders;
        private int activeOrders;
        private double fillRate;
        private BigDecimal averageSlippage;
    }
}