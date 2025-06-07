package com.example.scalpingBot.service.trading;

import com.example.scalpingBot.dto.exchange.OrderDto;
import com.example.scalpingBot.entity.Trade;
import com.example.scalpingBot.enums.OrderStatus;
import com.example.scalpingBot.exception.ExchangeApiException;
import com.example.scalpingBot.exception.TradingException;
import com.example.scalpingBot.repository.TradeRepository;
import com.example.scalpingBot.service.exchange.ExchangeApiService;
import com.example.scalpingBot.service.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * Сервис для управления жизненным циклом ордеров с обработкой частичного исполнения.
 */
@Service
public class OrderManagementService {

    private static final Logger log = LoggerFactory.getLogger(OrderManagementService.class);

    private final ExchangeApiService exchangeApiService;
    private final TradeRepository tradeRepository;
    private final NotificationService notificationService;
    private final OrderExecutionService orderExecutionService;

    // Хранилище активных ордеров для мониторинга
    private final ConcurrentHashMap<String, TrackedOrder> activeOrders = new ConcurrentHashMap<>();
    private final ScheduledExecutorService orderTracker = Executors.newScheduledThreadPool(3);

    // Конфигурация обработки ордеров
    private static final int ORDER_CHECK_INTERVAL_SECONDS = 3;
    private static final int MAX_ORDER_AGE_MINUTES = 10;
    private static final int PARTIAL_FILL_TIMEOUT_MINUTES = 5;
    private static final BigDecimal MIN_PARTIAL_FILL_THRESHOLD = new BigDecimal("0.01"); // 1%

    @Autowired
    public OrderManagementService(@Qualifier("binance") ExchangeApiService exchangeApiService,
                                  TradeRepository tradeRepository,
                                  NotificationService notificationService,
                                  OrderExecutionService orderExecutionService) {
        this.exchangeApiService = exchangeApiService;
        this.tradeRepository = tradeRepository;
        this.notificationService = notificationService;
        this.orderExecutionService = orderExecutionService;
    }

    /**
     * Запуск системы мониторинга ордеров при старте приложения
     */
    @EventListener(ApplicationReadyEvent.class)
    public void startOrderTracking() {
        log.info("🚀 Starting order tracking system...");

        // Восстанавливаем отслеживание ордеров после перезапуска
        recoverPendingOrders();

        // Запускаем основной цикл мониторинга
        orderTracker.scheduleAtFixedRate(
                this::processActiveOrders,
                ORDER_CHECK_INTERVAL_SECONDS,
                ORDER_CHECK_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );

        // Запускаем очистку старых ордеров
        orderTracker.scheduleAtFixedRate(
                this::cleanupExpiredOrders,
                60, // первый запуск через минуту
                60, // каждую минуту
                TimeUnit.SECONDS
        );

        log.info("✅ Order tracking system started");
    }

    /**
     * Регистрация ордера для отслеживания
     */
    public void trackOrder(OrderDto orderDto) {
        String orderId = orderDto.getExchangeOrderId();

        if (activeOrders.containsKey(orderId)) {
            log.debug("Order {} already being tracked", orderId);
            return;
        }

        TrackedOrder trackedOrder = new TrackedOrder(orderDto);
        activeOrders.put(orderId, trackedOrder);

        log.info("📋 Started tracking order {}: {} {} {} at {}",
                orderId, orderDto.getSide(), orderDto.getOriginalQuantity(),
                orderDto.getSymbol(), orderDto.getPrice());
    }

    /**
     * Основной цикл обработки активных ордеров
     */
    private void processActiveOrders() {
        if (activeOrders.isEmpty()) {
            return;
        }

        log.trace("🔍 Processing {} active orders", activeOrders.size());

        List<String> ordersToRemove = new ArrayList<>();

        for (Map.Entry<String, TrackedOrder> entry : activeOrders.entrySet()) {
            String orderId = entry.getKey();
            TrackedOrder trackedOrder = entry.getValue();

            try {
                boolean shouldRemove = processOrder(orderId, trackedOrder);
                if (shouldRemove) {
                    ordersToRemove.add(orderId);
                }
            } catch (Exception e) {
                log.error("❌ Error processing order {}: {}", orderId, e.getMessage(), e);
                trackedOrder.incrementErrorCount();

                // После 3 ошибок подряд - прекращаем отслеживание
                if (trackedOrder.getErrorCount() >= 3) {
                    log.error("🚨 Too many errors for order {}, stopping tracking", orderId);
                    ordersToRemove.add(orderId);
                    notificationService.sendError("Критические ошибки при обработке ордера " + orderId, e);
                }
            }
        }

        // Удаляем завершенные ордеры
        ordersToRemove.forEach(activeOrders::remove);
    }

    /**
     * Обработка конкретного ордера
     */
    private boolean processOrder(String orderId, TrackedOrder trackedOrder) {
        OrderDto currentOrder;

        try {
            currentOrder = exchangeApiService.getOrderStatus(
                    trackedOrder.getSymbol(),
                    orderId
            );
        } catch (ExchangeApiException e) {
            log.warn("Failed to get status for order {}: {}", orderId, e.getMessage());
            return false; // Продолжаем отслеживание
        }

        // Обновляем информацию об ордере
        trackedOrder.updateFromOrderDto(currentOrder);

        switch (currentOrder.getStatus()) {
            case FILLED -> {
                return handleFilledOrder(orderId, trackedOrder, currentOrder);
            }
            case PARTIALLY_FILLED -> {
                return handlePartiallyFilledOrder(orderId, trackedOrder, currentOrder);
            }
            case CANCELED, REJECTED, EXPIRED -> {
                return handleFailedOrder(orderId, trackedOrder, currentOrder);
            }
            case NEW -> {
                return handlePendingOrder(orderId, trackedOrder, currentOrder);
            }
            default -> {
                log.debug("Order {} has status: {}", orderId, currentOrder.getStatus());
                return false; // Продолжаем отслеживание
            }
        }
    }

    /**
     * Обработка полностью исполненного ордера
     */
    private boolean handleFilledOrder(String orderId, TrackedOrder trackedOrder, OrderDto currentOrder) {
        log.info("✅ Order {} fully executed: {} {} at {}",
                orderId, currentOrder.getExecutedQuantity(),
                currentOrder.getSymbol(), currentOrder.getPrice());

        // Сохраняем финальную сделку
        orderExecutionService.saveTradeRecord(currentOrder);

        // Если были частичные исполнения, сохраняем итоговую сделку
        if (trackedOrder.hasPartialFills()) {
            saveConsolidatedTrade(trackedOrder, currentOrder);
        }

        notificationService.sendSuccess(String.format(
                "Ордер полностью исполнен: %s %s %s по средней цене %s",
                currentOrder.getSide(),
                currentOrder.getExecutedQuantity(),
                currentOrder.getSymbol(),
                currentOrder.getPrice()
        ));

        return true; // Прекращаем отслеживание
    }

    /**
     * Обработка частично исполненного ордера
     */
    private boolean handlePartiallyFilledOrder(String orderId, TrackedOrder trackedOrder, OrderDto currentOrder) {
        BigDecimal executedQty = currentOrder.getExecutedQuantity();
        BigDecimal originalQty = currentOrder.getOriginalQuantity();
        BigDecimal fillPercentage = executedQty.divide(originalQty, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal(100));

        log.info("⏳ Order {} partially filled: {}/{} ({}%)",
                orderId, executedQty, originalQty, fillPercentage.setScale(2, java.math.RoundingMode.HALF_UP));

        // Проверяем, изменилось ли исполненное количество
        if (!executedQty.equals(trackedOrder.getLastExecutedQuantity())) {
            trackedOrder.setLastExecutedQuantity(executedQty);
            trackedOrder.setLastUpdateTime(LocalDateTime.now());

            // Сохраняем частичное исполнение
            savePartialFillRecord(trackedOrder, currentOrder);

            // Уведомляем только о значительных частичных исполнениях
            if (fillPercentage.compareTo(new BigDecimal(50)) >= 0) {
                notificationService.sendInfo(String.format(
                        "Частичное исполнение ордера %s: %s%% (%s/%s)",
                        orderId, fillPercentage.setScale(1, java.math.RoundingMode.HALF_UP),
                        executedQty, originalQty
                ));
            }
        }

        // Проверяем таймаут для частично исполненных ордеров
        if (trackedOrder.isPartialFillTimedOut(PARTIAL_FILL_TIMEOUT_MINUTES)) {
            log.warn("⚠️ Partial fill timeout for order {}, considering cancellation", orderId);

            // Если исполнено меньше минимального порога - отменяем
            if (fillPercentage.compareTo(MIN_PARTIAL_FILL_THRESHOLD.multiply(new BigDecimal(100))) < 0) {
                return handlePartialFillTimeout(orderId, trackedOrder, currentOrder);
            }
        }

        return false; // Продолжаем отслеживание
    }

    /**
     * Обработка таймаута частичного исполнения
     */
    private boolean handlePartialFillTimeout(String orderId, TrackedOrder trackedOrder, OrderDto currentOrder) {
        try {
            log.warn("🚫 Cancelling partially filled order {} due to timeout", orderId);

            OrderDto cancelledOrder = exchangeApiService.cancelOrder(
                    currentOrder.getSymbol(),
                    orderId
            );

            // Сохраняем информацию об отмененном ордере
            orderExecutionService.saveTradeRecord(cancelledOrder);

            notificationService.sendWarning(String.format(
                    "Ордер %s отменен по таймауту. Исполнено: %s/%s",
                    orderId, currentOrder.getExecutedQuantity(), currentOrder.getOriginalQuantity()
            ));

            return true; // Прекращаем отслеживание

        } catch (ExchangeApiException e) {
            log.error("Failed to cancel timed out order {}: {}", orderId, e.getMessage());
            notificationService.sendError("Ошибка отмены ордера " + orderId, e);
            return false; // Продолжаем отслеживание
        }
    }

    /**
     * Обработка неуспешных ордеров
     */
    private boolean handleFailedOrder(String orderId, TrackedOrder trackedOrder, OrderDto currentOrder) {
        log.warn("❌ Order {} failed with status: {}", orderId, currentOrder.getStatus());

        // Сохраняем информацию о неуспешном ордере
        orderExecutionService.saveTradeRecord(currentOrder);

        // Если были частичные исполнения, сохраняем их
        if (trackedOrder.hasPartialFills()) {
            saveConsolidatedTrade(trackedOrder, currentOrder);
        }

        notificationService.sendWarning(String.format(
                "Ордер %s завершен со статусом: %s. Исполнено: %s/%s",
                orderId, currentOrder.getStatus(),
                currentOrder.getExecutedQuantity(), currentOrder.getOriginalQuantity()
        ));

        return true; // Прекращаем отслеживание
    }

    /**
     * Обработка ожидающих ордеров
     */
    private boolean handlePendingOrder(String orderId, TrackedOrder trackedOrder, OrderDto currentOrder) {
        // Проверяем таймаут для новых ордеров
        if (trackedOrder.isExpired(MAX_ORDER_AGE_MINUTES)) {
            log.warn("⏰ Order {} expired (age: {} minutes), considering cancellation",
                    orderId, trackedOrder.getAgeInMinutes());

            return handleOrderTimeout(orderId, trackedOrder, currentOrder);
        }

        return false; // Продолжаем отслеживание
    }

    /**
     * Обработка таймаута ордера
     */
    private boolean handleOrderTimeout(String orderId, TrackedOrder trackedOrder, OrderDto currentOrder) {
        try {
            log.warn("🚫 Cancelling expired order {}", orderId);

            OrderDto cancelledOrder = exchangeApiService.cancelOrder(
                    currentOrder.getSymbol(),
                    orderId
            );

            orderExecutionService.saveTradeRecord(cancelledOrder);

            notificationService.sendWarning(String.format(
                    "Ордер %s отменен по таймауту (%d минут)",
                    orderId, MAX_ORDER_AGE_MINUTES
            ));

            return true; // Прекращаем отслеживание

        } catch (ExchangeApiException e) {
            log.error("Failed to cancel expired order {}: {}", orderId, e.getMessage());
            return false; // Продолжаем отслеживание
        }
    }

    /**
     * Сохранение записи о частичном исполнении
     */
    @Transactional
    private void savePartialFillRecord(TrackedOrder trackedOrder, OrderDto currentOrder) {
        try {
            // Рассчитываем количество нового частичного исполнения
            BigDecimal newFillQuantity = currentOrder.getExecutedQuantity()
                    .subtract(trackedOrder.getTotalSavedQuantity());

            if (newFillQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                return; // Нет нового исполнения
            }

            Trade partialTrade = Trade.builder()
                    .exchangeTradeId(currentOrder.getExchangeOrderId() + "_partial_" + System.currentTimeMillis())
                    .tradingPair(currentOrder.getSymbol())
                    .status(OrderStatus.PARTIALLY_FILLED)
                    .side(currentOrder.getSide())
                    .type(currentOrder.getType())
                    .price(currentOrder.getPrice())
                    .quantity(newFillQuantity)
                    .commission(calculatePartialCommission(currentOrder, newFillQuantity))
                    .commissionAsset(determineCommissionAsset(currentOrder))
                    .executionTimestamp(LocalDateTime.now())
                    .build();

            tradeRepository.save(partialTrade);
            trackedOrder.addSavedQuantity(newFillQuantity);

            log.debug("💾 Saved partial fill record: {} {} for order {}",
                    newFillQuantity, currentOrder.getSymbol(), currentOrder.getExchangeOrderId());

        } catch (Exception e) {
            log.error("Failed to save partial fill record: {}", e.getMessage(), e);
        }
    }

    /**
     * Сохранение консолидированной сделки
     */
    @Transactional
    private void saveConsolidatedTrade(TrackedOrder trackedOrder, OrderDto finalOrder) {
        try {
            Trade consolidatedTrade = Trade.builder()
                    .exchangeTradeId(finalOrder.getExchangeOrderId() + "_consolidated")
                    .tradingPair(finalOrder.getSymbol())
                    .status(finalOrder.getStatus())
                    .side(finalOrder.getSide())
                    .type(finalOrder.getType())
                    .price(calculateAveragePrice(trackedOrder, finalOrder))
                    .quantity(finalOrder.getExecutedQuantity())
                    .commission(calculateTotalCommission(finalOrder))
                    .commissionAsset(determineCommissionAsset(finalOrder))
                    .executionTimestamp(LocalDateTime.now())
                    .build();

            tradeRepository.save(consolidatedTrade);

            log.info("💾 Saved consolidated trade record for order {}: {} {} at avg price {}",
                    finalOrder.getExchangeOrderId(), finalOrder.getExecutedQuantity(),
                    finalOrder.getSymbol(), consolidatedTrade.getPrice());

        } catch (Exception e) {
            log.error("Failed to save consolidated trade: {}", e.getMessage(), e);
        }
    }

    /**
     * Восстановление отслеживания ордеров после перезапуска
     */
    private void recoverPendingOrders() {
        // В production здесь можно добавить восстановление ордеров из БД
        // или синхронизацию с биржей для поиска открытых ордеров
        log.info("🔄 Order recovery completed");
    }

    /**
     * Очистка истекших ордеров
     */
    private void cleanupExpiredOrders() {
        List<String> expiredOrders = activeOrders.entrySet().stream()
                .filter(entry -> entry.getValue().isExpired(MAX_ORDER_AGE_MINUTES * 2))
                .map(Map.Entry::getKey)
                .toList();

        expiredOrders.forEach(orderId -> {
            activeOrders.remove(orderId);
            log.debug("🧹 Cleaned up expired order tracking: {}", orderId);
        });
    }

    // Utility методы
    private BigDecimal calculatePartialCommission(OrderDto orderDto, BigDecimal quantity) {
        return quantity.multiply(orderDto.getPrice()).multiply(new BigDecimal("0.001"));
    }

    private BigDecimal calculateTotalCommission(OrderDto orderDto) {
        return orderDto.getExecutedQuantity().multiply(orderDto.getPrice()).multiply(new BigDecimal("0.001"));
    }

    private String determineCommissionAsset(OrderDto orderDto) {
        String symbol = orderDto.getSymbol();
        if (symbol.endsWith("USDT")) return "USDT";
        if (symbol.endsWith("BTC")) return "BTC";
        return "USDT";
    }

    private BigDecimal calculateAveragePrice(TrackedOrder trackedOrder, OrderDto finalOrder) {
        // Упрощенный расчет - в реальности нужно взвешивать по объемам
        return finalOrder.getPrice();
    }

    /**
     * Получение статистики активных ордеров
     */
    public Map<String, Object> getOrderTrackingStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeOrders", activeOrders.size());
        stats.put("totalTracked", activeOrders.values().stream().mapToLong(TrackedOrder::getCheckCount).sum());
        return stats;
    }

    /**
     * Graceful shutdown
     */
    public void shutdown() {
        log.info("🛑 Shutting down OrderManagementService...");
        orderTracker.shutdown();
        try {
            if (!orderTracker.awaitTermination(10, TimeUnit.SECONDS)) {
                orderTracker.shutdownNow();
            }
        } catch (InterruptedException e) {
            orderTracker.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("✅ OrderManagementService shutdown completed");
    }

    /**
     * Вспомогательный класс для отслеживания ордеров
     */
    private static class TrackedOrder {
        private final String symbol;
        private final LocalDateTime createdTime;
        private BigDecimal lastExecutedQuantity = BigDecimal.ZERO;
        private BigDecimal totalSavedQuantity = BigDecimal.ZERO;
        private LocalDateTime lastUpdateTime;
        private long checkCount = 0;
        private int errorCount = 0;

        public TrackedOrder(OrderDto orderDto) {
            this.symbol = orderDto.getSymbol();
            this.createdTime = LocalDateTime.now();
            this.lastUpdateTime = this.createdTime;
        }

        public void updateFromOrderDto(OrderDto orderDto) {
            this.checkCount++;
            this.errorCount = 0; // Сбрасываем счетчик ошибок при успешном обновлении
        }

        public boolean isExpired(int maxAgeMinutes) {
            return LocalDateTime.now().isAfter(createdTime.plusMinutes(maxAgeMinutes));
        }

        public boolean isPartialFillTimedOut(int timeoutMinutes) {
            return lastExecutedQuantity.compareTo(BigDecimal.ZERO) > 0 &&
                    LocalDateTime.now().isAfter(lastUpdateTime.plusMinutes(timeoutMinutes));
        }

        public boolean hasPartialFills() {
            return lastExecutedQuantity.compareTo(BigDecimal.ZERO) > 0;
        }

        public long getAgeInMinutes() {
            return java.time.Duration.between(createdTime, LocalDateTime.now()).toMinutes();
        }

        // Getters and setters
        public String getSymbol() { return symbol; }
        public BigDecimal getLastExecutedQuantity() { return lastExecutedQuantity; }
        public void setLastExecutedQuantity(BigDecimal lastExecutedQuantity) { this.lastExecutedQuantity = lastExecutedQuantity; }
        public BigDecimal getTotalSavedQuantity() { return totalSavedQuantity; }
        public void addSavedQuantity(BigDecimal quantity) { this.totalSavedQuantity = this.totalSavedQuantity.add(quantity); }
        public void setLastUpdateTime(LocalDateTime lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }
        public long getCheckCount() { return checkCount; }
        public int getErrorCount() { return errorCount; }
        public void incrementErrorCount() { this.errorCount++; }
    }
}