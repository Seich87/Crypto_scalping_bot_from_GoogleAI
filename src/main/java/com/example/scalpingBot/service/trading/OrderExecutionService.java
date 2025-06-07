package com.example.scalpingBot.service.trading;

import com.example.scalpingBot.dto.exchange.OrderDto;
import com.example.scalpingBot.entity.Trade;
import com.example.scalpingBot.entity.TradingPair;
import com.example.scalpingBot.enums.OrderSide;
import com.example.scalpingBot.enums.OrderStatus;
import com.example.scalpingBot.enums.OrderType;
import com.example.scalpingBot.exception.ExchangeApiException;
import com.example.scalpingBot.exception.TradingException;
import com.example.scalpingBot.repository.TradeRepository;
import com.example.scalpingBot.repository.TradingPairRepository;
import com.example.scalpingBot.service.exchange.ExchangeApiService;
import com.example.scalpingBot.service.notification.NotificationService;
import com.example.scalpingBot.utils.MathUtils;
import com.example.scalpingBot.utils.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Улучшенный сервис для исполнения ордеров с полной обработкой жизненного цикла.
 * Включает валидацию, сохранение сделок и интеграцию с OrderManagementService.
 */
@Service
public class OrderExecutionService {

    private static final Logger log = LoggerFactory.getLogger(OrderExecutionService.class);

    private final ExchangeApiService exchangeApiService;
    private final TradeRepository tradeRepository;
    private final TradingPairRepository tradingPairRepository;
    private final NotificationService notificationService;
    private final OrderManagementService orderManagementService;
    private final ScheduledExecutorService orderMonitorExecutor;

    // Конфигурация мониторинга ордеров
    private static final int ORDER_CHECK_INTERVAL_SECONDS = 5;
    private static final int MAX_ORDER_WAIT_MINUTES = 5;
    private static final int MAX_PARTIAL_FILL_CHECKS = 20;

    @Autowired
    public OrderExecutionService(@Qualifier("binance") ExchangeApiService exchangeApiService,
                                 TradeRepository tradeRepository,
                                 TradingPairRepository tradingPairRepository,
                                 NotificationService notificationService,
                                 OrderManagementService orderManagementService) {
        this.exchangeApiService = exchangeApiService;
        this.tradeRepository = tradeRepository;
        this.tradingPairRepository = tradingPairRepository;
        this.notificationService = notificationService;
        this.orderManagementService = orderManagementService;
        this.orderMonitorExecutor = Executors.newScheduledThreadPool(2);
    }

    /**
     * Исполняет торговую операцию с полной валидацией и мониторингом.
     *
     * @param symbol   Торговая пара.
     * @param side     Сторона сделки (BUY или SELL).
     * @param quantity Количество актива для покупки/продажи.
     * @return OrderDto с информацией о размещенном ордере.
     */
    @Transactional
    public OrderDto executeOrder(String symbol, OrderSide side, BigDecimal quantity) {
        log.info("🔄 Starting order execution for {} {} of {}", side, quantity.toPlainString(), symbol);

        // 1. Валидация входных параметров
        validateOrderParameters(symbol, side, quantity);

        // 2. Получение и валидация конфигурации торговой пары
        TradingPair tradingPair = validateAndGetTradingPair(symbol);

        // 3. Округление количества согласно точности пары
        BigDecimal adjustedQuantity = adjustQuantityPrecision(quantity, tradingPair);

        // 4. Выбор типа ордера и исполнение
        OrderType orderType = determineOrderType(symbol, side, adjustedQuantity);
        BigDecimal price = (orderType == OrderType.LIMIT) ? calculateLimitPrice(symbol, side) : null;

        try {
            // 5. Размещение ордера на бирже
            OrderDto placedOrder = exchangeApiService.placeOrder(symbol, side, orderType, adjustedQuantity, price);
            log.info("✅ Order placed successfully. Exchange Order ID: {}, Status: {}",
                    placedOrder.getExchangeOrderId(), placedOrder.getStatus());

            // 6. Регистрируем ордер для отслеживания в OrderManagementService
            orderManagementService.trackOrder(placedOrder);

            return placedOrder;

        } catch (ExchangeApiException e) {
            log.error("❌ Failed to execute order for {}. Reason: {}", symbol, e.getMessage());
            notificationService.sendError("Ошибка исполнения ордера для " + symbol, e);
            throw e;
        }
    }

    /**
     * Отменяет существующий ордер на бирже с сохранением информации.
     */
    @Transactional
    public OrderDto cancelOrder(String symbol, String orderId) {
        log.info("🚫 Attempting to cancel order {} for {}", orderId, symbol);

        try {
            OrderDto cancelledOrder = exchangeApiService.cancelOrder(symbol, orderId);
            log.info("✅ Successfully cancelled order {}. Final status: {}", orderId, cancelledOrder.getStatus());

            // Сохраняем информацию об отмененном ордере
            if (cancelledOrder.getStatus() == OrderStatus.CANCELED) {
                saveTradeRecord(cancelledOrder);
            }

            return cancelledOrder;

        } catch (ExchangeApiException e) {
            log.error("❌ Failed to cancel order {} for {}. Reason: {}", orderId, symbol, e.getMessage());
            notificationService.sendError("Ошибка отмены ордера " + orderId, e);
            throw e;
        }
    }

    /**
     * Валидация параметров ордера
     */
    private void validateOrderParameters(String symbol, OrderSide side, BigDecimal quantity) {
        ValidationUtils.assertNotBlank(symbol, "Trading pair symbol cannot be blank");
        ValidationUtils.assertNotNull(side, "Order side cannot be null");
        ValidationUtils.assertPositive(quantity, "Order quantity must be positive");
    }

    /**
     * Получение и валидация конфигурации торговой пары
     */
    private TradingPair validateAndGetTradingPair(String symbol) {
        Optional<TradingPair> tradingPairOpt = tradingPairRepository.findBySymbol(symbol);

        if (tradingPairOpt.isEmpty()) {
            throw new TradingException("Trading pair configuration not found for: " + symbol);
        }

        TradingPair tradingPair = tradingPairOpt.get();

        if (!tradingPair.isActive()) {
            throw new TradingException("Trading pair " + symbol + " is not active for trading");
        }

        return tradingPair;
    }

    /**
     * Округление количества согласно точности торговой пары
     */
    private BigDecimal adjustQuantityPrecision(BigDecimal quantity, TradingPair tradingPair) {
        BigDecimal adjustedQuantity = MathUtils.scale(quantity, tradingPair.getQuantityPrecision());

        // Проверка минимального размера ордера
        if (tradingPair.getMinOrderSize() != null) {
            BigDecimal estimatedValue = adjustedQuantity.multiply(BigDecimal.valueOf(50000)); // Примерная цена для проверки
            if (estimatedValue.compareTo(tradingPair.getMinOrderSize()) < 0) {
                throw new TradingException(String.format(
                        "Order size too small. Estimated value: %s, minimum required: %s",
                        estimatedValue.toPlainString(), tradingPair.getMinOrderSize().toPlainString()
                ));
            }
        }

        log.debug("Adjusted quantity from {} to {} for precision {}",
                quantity.toPlainString(), adjustedQuantity.toPlainString(), tradingPair.getQuantityPrecision());

        return adjustedQuantity;
    }

    /**
     * Определение оптимального типа ордера
     */
    private OrderType determineOrderType(String symbol, OrderSide side, BigDecimal quantity) {
        // Для скальпинга обычно используем MARKET для быстроты
        // В будущем можно добавить логику выбора между MARKET и LIMIT
        // на основе волатильности, спреда и размера ордера
        return OrderType.MARKET;
    }

    /**
     * Расчет цены для лимитного ордера (если используется)
     */
    private BigDecimal calculateLimitPrice(String symbol, OrderSide side) {
        try {
            var ticker = exchangeApiService.getTicker(symbol);
            BigDecimal marketPrice = ticker.getLastPrice();

            // Для покупки ставим цену чуть ниже market, для продажи - чуть выше
            BigDecimal adjustment = side == OrderSide.BUY ?
                    new BigDecimal("-0.001") : new BigDecimal("0.001"); // 0.1%

            return MathUtils.applyPercentage(marketPrice, adjustment);

        } catch (Exception e) {
            log.warn("Failed to calculate limit price for {}, fallback to market order", symbol);
            return null;
        }
    }

    /**
     * Мониторинг исполнения ордера в асинхронном режиме
     * DEPRECATED: Теперь эту функцию выполняет OrderManagementService
     */
    @Deprecated
    private void monitorOrderExecution(OrderDto initialOrder) {
        CompletableFuture.runAsync(() -> {
            String orderId = initialOrder.getExchangeOrderId();
            String symbol = initialOrder.getSymbol();
            int checkCount = 0;

            log.debug("🔍 Starting order monitoring for {} ({})", orderId, symbol);

            try {
                while (checkCount < MAX_PARTIAL_FILL_CHECKS) {
                    Thread.sleep(ORDER_CHECK_INTERVAL_SECONDS * 1000L);
                    checkCount++;

                    OrderDto currentOrder = exchangeApiService.getOrderStatus(symbol, orderId);

                    switch (currentOrder.getStatus()) {
                        case FILLED -> {
                            log.info("✅ Order {} fully executed", orderId);
                            saveTradeRecord(currentOrder);
                            notificationService.sendSuccess(String.format(
                                    "Ордер полностью исполнен: %s %s %s по цене %s",
                                    currentOrder.getSide(), currentOrder.getExecutedQuantity(),
                                    symbol, currentOrder.getPrice()
                            ));
                            return; // Завершаем мониторинг
                        }
                        case PARTIALLY_FILLED -> {
                            log.info("⏳ Order {} partially filled: {}/{}",
                                    orderId, currentOrder.getExecutedQuantity(), currentOrder.getOriginalQuantity());
                            // Продолжаем мониторинг
                        }
                        case CANCELED, REJECTED, EXPIRED -> {
                            log.warn("❌ Order {} ended with status: {}", orderId, currentOrder.getStatus());
                            saveTradeRecord(currentOrder);
                            notificationService.sendWarning(String.format(
                                    "Ордер %s завершен со статусом: %s", orderId, currentOrder.getStatus()
                            ));
                            return; // Завершаем мониторинг
                        }
                        case NEW -> {
                            log.debug("⏳ Order {} still pending execution", orderId);
                        }
                    }
                }

                // Если достигли максимального количества проверок
                log.warn("⚠️ Order monitoring timeout for {} after {} checks", orderId, MAX_PARTIAL_FILL_CHECKS);
                OrderDto finalOrder = exchangeApiService.getOrderStatus(symbol, orderId);
                saveTradeRecord(finalOrder);

            } catch (Exception e) {
                log.error("❌ Error during order monitoring for {}: {}", orderId, e.getMessage(), e);
                notificationService.sendError("Ошибка мониторинга ордера " + orderId, e);
            }
        }, orderMonitorExecutor);
    }

    /**
     * Сохранение записи о сделке в базу данных
     */
    @Transactional
    public void saveTradeRecord(OrderDto orderDto) {
        try {
            // Проверяем, не сохранена ли уже эта сделка
            Optional<Trade> existingTrade = tradeRepository.findByExchangeTradeId(orderDto.getExchangeOrderId());
            if (existingTrade.isPresent()) {
                log.debug("Trade record already exists for order {}", orderDto.getExchangeOrderId());
                return;
            }

            Trade trade = Trade.builder()
                    .exchangeTradeId(orderDto.getExchangeOrderId())
                    .tradingPair(orderDto.getSymbol())
                    .status(orderDto.getStatus())
                    .side(orderDto.getSide())
                    .type(orderDto.getType())
                    .price(orderDto.getPrice())
                    .quantity(orderDto.getExecutedQuantity()) // Используем исполненное количество
                    .commission(calculateCommission(orderDto))
                    .commissionAsset(determineCommissionAsset(orderDto))
                    .executionTimestamp(orderDto.getUpdatedAt() != null ? orderDto.getUpdatedAt() : LocalDateTime.now())
                    .build();

            tradeRepository.save(trade);

            log.info("💾 Saved trade record: {} {} {} at {}",
                    trade.getSide(), trade.getQuantity(), trade.getTradingPair(), trade.getPrice());

        } catch (Exception e) {
            log.error("❌ Failed to save trade record for order {}: {}",
                    orderDto.getExchangeOrderId(), e.getMessage(), e);
            // Не пробрасываем исключение, чтобы не нарушить основной flow
        }
    }

    /**
     * Расчет комиссии (заглушка - зависит от биржи)
     */
    private BigDecimal calculateCommission(OrderDto orderDto) {
        // Примерная комиссия 0.1% для Binance
        return orderDto.getExecutedQuantity()
                .multiply(orderDto.getPrice())
                .multiply(new BigDecimal("0.001"));
    }

    /**
     * Определение актива комиссии
     */
    private String determineCommissionAsset(OrderDto orderDto) {
        // Обычно комиссия берется в quote валюте (USDT) или BNB
        String symbol = orderDto.getSymbol();
        if (symbol.endsWith("USDT")) return "USDT";
        if (symbol.endsWith("BTC")) return "BTC";
        return "USDT"; // По умолчанию
    }

    /**
     * Graceful shutdown
     */
    public void shutdown() {
        log.info("🛑 Shutting down OrderExecutionService...");
        orderMonitorExecutor.shutdown();
        try {
            if (!orderMonitorExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                orderMonitorExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            orderMonitorExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("✅ OrderExecutionService shutdown completed");
    }
}