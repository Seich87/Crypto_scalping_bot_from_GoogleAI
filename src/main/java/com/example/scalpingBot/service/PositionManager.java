package com.example.scalpingBot.service;

import com.example.scalpingBot.config.TradingConfig;
import com.example.scalpingBot.entity.Position;
import com.example.scalpingBot.entity.Trade;
import com.example.scalpingBot.enums.OrderSide;
import com.example.scalpingBot.enums.RiskLevel;
import com.example.scalpingBot.enums.TradingPairType;
import com.example.scalpingBot.repository.PositionRepository;
import com.example.scalpingBot.utils.DateUtils;
import com.example.scalpingBot.utils.MathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Сервис управления торговыми позициями скальпинг-бота
 *
 * Основные функции:
 * - Создание и закрытие позиций на основе торговых операций
 * - Обновление P&L позиций в реальном времени
 * - Контроль времени удержания (максимум 1 час для скальпинга)
 * - Управление стоп-лоссами и тейк-профитами
 * - Автоматическое закрытие просроченных позиций
 * - Расчет корреляций между позициями
 * - Мониторинг производительности позиций
 *
 * Все операции оптимизированы для скальпинг-стратегии
 * с акцентом на быстрое открытие/закрытие позиций.
 *
 * @author ScalpingBot Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PositionManager {

    private final PositionRepository positionRepository;
    private final TradingConfig tradingConfig;
    private final MarketDataService marketDataService;
    private final NotificationService notificationService;

    /**
     * Кеш активных позиций для быстрого доступа
     */
    private final Map<String, Position> activePositionsCache = new ConcurrentHashMap<>();

    /**
     * Кеш последних цен для обновления P&L
     */
    private final Map<String, BigDecimal> lastPricesCache = new ConcurrentHashMap<>();

    /**
     * Создать позицию на основе исполненного ордера
     *
     * @param trade исполненная торговая операция
     * @return созданная позиция
     */
    @Transactional
    public Position createPositionFromTrade(Trade trade) {
        try {
            log.info("Creating position from trade: {} {} {} at {}",
                    trade.getOrderSide(), trade.getQuantity(), trade.getTradingPair(), trade.getAvgPrice());

            // Проверяем, есть ли уже активная позиция по этой паре
            Optional<Position> existingPosition = positionRepository
                    .findByTradingPairAndIsActiveTrue(trade.getTradingPair());

            if (existingPosition.isPresent()) {
                // Обновляем существующую позицию
                return updateExistingPosition(existingPosition.get(), trade);
            } else {
                // Создаем новую позицию
                return createNewPosition(trade);
            }

        } catch (Exception e) {
            log.error("Failed to create position from trade {}: {}", trade.getId(), e.getMessage());
            throw new RuntimeException("Failed to create position: " + e.getMessage(), e);
        }
    }

    /**
     * Создать новую позицию
     *
     * @param trade торговая операция
     * @return новая позиция
     */
    private Position createNewPosition(Trade trade) {
        BigDecimal entryValue = trade.getQuantity().multiply(trade.getAvgPrice());

        Position position = Position.builder()
                .tradingPair(trade.getTradingPair())
                .pairType(TradingPairType.fromPairName(trade.getTradingPair()))
                .side(trade.getOrderSide())
                .status(Position.PositionStatus.OPEN)
                .isActive(true)
                .size(trade.getQuantity())
                .entryPrice(trade.getAvgPrice())
                .currentPrice(trade.getAvgPrice())
                .entryValue(entryValue)
                .currentValue(entryValue)
                .stopLossPrice(trade.getStopLossPrice())
                .takeProfitPrice(trade.getTakeProfitPrice())
                .totalCommission(trade.getCommission())
                .riskLevel(RiskLevel.MEDIUM)
                .strategyName(trade.getStrategyName())
                .exchangeName(trade.getExchangeName())
                .openedAt(DateUtils.nowMoscow())
                .maxHoldingTimeMinutes(tradingConfig.getStrategy().getMaxPositionTimeMinutes())
                .tradesCount(1)
                .entrySignalStrength(trade.getSignalStrength())
                .build();

        // Рассчитываем процент от портфеля
        // Здесь должен быть вызов к RiskManager для получения баланса
        BigDecimal portfolioValue = new BigDecimal("10000"); // Заглушка
        position.setPortfolioPercent(entryValue.divide(portfolioValue, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100")));

        // Сохраняем позицию
        position = positionRepository.save(position);

        // Добавляем в кеш
        activePositionsCache.put(trade.getTradingPair(), position);

        // Связываем торговую операцию с позицией
        trade.setPositionId(position.getId());

        log.info("Created new position {}: {} {} {} at {}",
                position.getId(), position.getSide(), position.getSize(),
                position.getTradingPair(), position.getEntryPrice());

        return position;
    }

    /**
     * Обновить существующую позицию
     *
     * @param position существующая позиция
     * @param trade новая торговая операция
     * @return обновленная позиция
     */
    private Position updateExistingPosition(Position position, Trade trade) {
        log.info("Updating existing position {} with trade {}", position.getId(), trade.getId());

        // Если направления совпадают - увеличиваем позицию
        if (position.getSide() == trade.getOrderSide()) {
            return increasePosition(position, trade);
        } else {
            // Если направления противоположны - уменьшаем или закрываем позицию
            return decreasePosition(position, trade);
        }
    }

    /**
     * Увеличить размер позиции
     *
     * @param position позиция
     * @param trade торговая операция
     * @return обновленная позиция
     */
    private Position increasePosition(Position position, Trade trade) {
        BigDecimal oldSize = position.getSize();
        BigDecimal newSize = oldSize.add(trade.getQuantity());

        // Рассчитываем новую среднюю цену входа
        BigDecimal oldValue = oldSize.multiply(position.getEntryPrice());
        BigDecimal newValue = trade.getQuantity().multiply(trade.getAvgPrice());
        BigDecimal totalValue = oldValue.add(newValue);
        BigDecimal newAvgPrice = totalValue.divide(newSize, 8, BigDecimal.ROUND_HALF_UP);

        // Обновляем позицию
        position.setSize(newSize);
        position.setEntryPrice(newAvgPrice);
        position.setEntryValue(totalValue);
        position.setCurrentValue(newSize.multiply(position.getCurrentPrice()));
        position.setTotalCommission(position.getTotalCommission().add(trade.getCommission()));
        position.setTradesCount(position.getTradesCount() + 1);

        // Пересчитываем P&L
        updatePositionPnL(position);

        position = positionRepository.save(position);
        activePositionsCache.put(position.getTradingPair(), position);

        log.info("Increased position {}: {} → {} at avg price {}",
                position.getId(), oldSize, newSize, newAvgPrice);

        return position;
    }

    /**
     * Уменьшить размер позиции или закрыть ее
     *
     * @param position позиция
     * @param trade торговая операция
     * @return обновленная позиция
     */
    private Position decreasePosition(Position position, Trade trade) {
        BigDecimal currentSize = position.getSize();
        BigDecimal closeSize = trade.getQuantity();

        if (closeSize.compareTo(currentSize) >= 0) {
            // Полное закрытие позиции
            return closePosition(position, trade.getAvgPrice(), "Fully closed by opposite trade");
        } else {
            // Частичное закрытие позиции
            return partiallyClosePosition(position, trade);
        }
    }

    /**
     * Частично закрыть позицию
     *
     * @param position позиция
     * @param trade торговая операция
     * @return обновленная позиция
     */
    private Position partiallyClosePosition(Position position, Trade trade) {
        BigDecimal closedSize = trade.getQuantity();
        BigDecimal remainingSize = position.getSize().subtract(closedSize);

        // Рассчитываем реализованный P&L для закрываемой части
        BigDecimal realizedPnl = calculatePartialPnL(position, trade, closedSize);

        // Обновляем позицию
        position.setSize(remainingSize);
        position.setRealizedPnl((position.getRealizedPnl() != null ? position.getRealizedPnl() : BigDecimal.ZERO)
                .add(realizedPnl));
        position.setTotalCommission(position.getTotalCommission().add(trade.getCommission()));
        position.setTradesCount(position.getTradesCount() + 1);
        position.setStatus(Position.PositionStatus.REDUCING);

        // Пересчитываем нереализованный P&L для оставшейся части
        updatePositionPnL(position);

        position = positionRepository.save(position);
        activePositionsCache.put(position.getTradingPair(), position);

        log.info("Partially closed position {}: {} → {} (closed {}), realized P&L: {}",
                position.getId(), position.getSize().add(closedSize), remainingSize, closedSize, realizedPnl);

        return position;
    }

    /**
     * Рассчитать частичный P&L
     *
     * @param position позиция
     * @param trade торговая операция закрытия
     * @param closedSize закрываемый размер
     * @return реализованный P&L
     */
    private BigDecimal calculatePartialPnL(Position position, Trade trade, BigDecimal closedSize) {
        BigDecimal entryPrice = position.getEntryPrice();
        BigDecimal exitPrice = trade.getAvgPrice();

        int sideMultiplier = position.getSide() == OrderSide.BUY ? 1 : -1;
        BigDecimal priceDiff = exitPrice.subtract(entryPrice);

        return closedSize.multiply(priceDiff).multiply(new BigDecimal(sideMultiplier));
    }

    /**
     * Закрыть позицию полностью
     *
     * @param position позиция
     * @param exitPrice цена закрытия
     * @param reason причина закрытия
     * @return закрытая позиция
     */
    @Transactional
    public Position closePosition(Position position, BigDecimal exitPrice, String reason) {
        try {
            log.info("Closing position {}: {} {} at {} (reason: {})",
                    position.getId(), position.getSide(), position.getTradingPair(), exitPrice, reason);

            // Обновляем позицию
            position.closePosition(exitPrice, reason);

            // Рассчитываем финальные метрики
            calculateFinalMetrics(position);

            // Сохраняем в БД
            position = positionRepository.save(position);

            // Удаляем из кеша активных позиций
            activePositionsCache.remove(position.getTradingPair());

            // Отправляем уведомление
            sendPositionClosedNotification(position);

            log.info("Position {} closed successfully. Final P&L: {} ({}%)",
                    position.getId(), position.getRealizedPnl(), position.getRealizedPnlPercent());

            return position;

        } catch (Exception e) {
            log.error("Failed to close position {}: {}", position.getId(), e.getMessage());
            throw new RuntimeException("Failed to close position: " + e.getMessage(), e);
        }
    }

    /**
     * Рассчитать финальные метрики позиции
     *
     * @param position закрываемая позиция
     */
    private void calculateFinalMetrics(Position position) {
        // Время удержания
        if (position.getClosedAt() != null && position.getOpenedAt() != null) {
            long holdingMinutes = java.time.Duration.between(position.getOpenedAt(), position.getClosedAt()).toMinutes();
            position.setTradesCount((int) holdingMinutes); // Временное решение, нужно добавить поле holdingTimeMinutes
        }

        // Процент от портфеля
        if (position.getRealizedPnlPercent() == null && position.getEntryValue() != null) {
            BigDecimal pnlPercent = position.getRealizedPnl()
                    .divide(position.getEntryValue(), 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(new BigDecimal("100"));
            position.setRealizedPnlPercent(pnlPercent);
        }
    }

    /**
     * Отправить уведомление о закрытии позиции
     *
     * @param position закрытая позиция
     */
    private void sendPositionClosedNotification(Position position) {
        try {
            String message = String.format(
                    "Position Closed:\n" +
                            "Pair: %s\n" +
                            "Side: %s\n" +
                            "Size: %s\n" +
                            "Entry: %s → Exit: %s\n" +
                            "P&L: %s USDT (%.2f%%)\n" +
                            "Duration: %d minutes\n" +
                            "Reason: %s",
                    position.getTradingPair(),
                    position.getSide(),
                    position.getSize(),
                    position.getEntryPrice(),
                    position.getExitPrice(),
                    position.getRealizedPnl(),
                    position.getRealizedPnlPercent(),
                    position.getHoldingTimeMinutes(),
                    position.getCloseReason()
            );

            if (position.isProfitable()) {
                notificationService.sendSuccessAlert("Profitable Position Closed", message);
            } else {
                notificationService.sendWarningAlert("Loss Position Closed", message);
            }

        } catch (Exception e) {
            log.error("Failed to send position closed notification: {}", e.getMessage());
        }
    }

    /**
     * Получить все активные позиции
     *
     * @return список активных позиций
     */
    public List<Position> getActivePositions() {
        return positionRepository.findByIsActiveTrueOrderByOpenedAtDesc();
    }

    /**
     * Проверить, есть ли активная позиция по торговой паре
     *
     * @param tradingPair торговая пара
     * @return true если есть активная позиция
     */
    public boolean hasActivePosition(String tradingPair) {
        // Сначала проверяем кеш
        if (activePositionsCache.containsKey(tradingPair)) {
            return true;
        }

        // Проверяем в БД
        return positionRepository.hasActivePositionForPair(tradingPair);
    }

    /**
     * Получить активную позицию по торговой паре
     *
     * @param tradingPair торговая пара
     * @return позиция или null
     */
    public Position getActivePosition(String tradingPair) {
        // Сначала проверяем кеш
        Position cachedPosition = activePositionsCache.get(tradingPair);
        if (cachedPosition != null) {
            return cachedPosition;
        }

        // Загружаем из БД и добавляем в кеш
        Optional<Position> position = positionRepository.findByTradingPairAndIsActiveTrue(tradingPair);
        if (position.isPresent()) {
            activePositionsCache.put(tradingPair, position.get());
            return position.get();
        }

        return null;
    }

    /**
     * Обновить P&L всех активных позиций
     */
    @Async
    public CompletableFuture<Void> updateAllPositionsPnL() {
        try {
            List<Position> activePositions = getActivePositions();

            if (activePositions.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }

            log.debug("Updating P&L for {} active positions", activePositions.size());

            for (Position position : activePositions) {
                try {
                    updatePositionWithMarketData(position);
                } catch (Exception e) {
                    log.error("Failed to update P&L for position {}: {}", position.getId(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Failed to update positions P&L: {}", e.getMessage());
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Обновить позицию с актуальными рыночными данными
     *
     * @param position позиция для обновления
     */
    private void updatePositionWithMarketData(Position position) {
        try {
            // Получаем текущую цену
            BigDecimal currentPrice = getCurrentPrice(position.getTradingPair(), position.getExchangeName());

            if (currentPrice != null) {
                // Обновляем цену и P&L
                position.updateCurrentPrice(currentPrice);

                // Сохраняем изменения
                positionRepository.save(position);

                // Обновляем кеш
                activePositionsCache.put(position.getTradingPair(), position);
            }

        } catch (Exception e) {
            log.error("Failed to update position {} with market data: {}", position.getId(), e.getMessage());
        }
    }

    /**
     * Получить текущую цену для торговой пары
     *
     * @param tradingPair торговая пара
     * @param exchangeName биржа
     * @return текущая цена
     */
    private BigDecimal getCurrentPrice(String tradingPair, String exchangeName) {
        try {
            // Проверяем кеш цен
            String cacheKey = tradingPair + "_" + exchangeName;
            BigDecimal cachedPrice = lastPricesCache.get(cacheKey);

            // Получаем свежие данные если кеш старый или пустой
            var marketData = marketDataService.getCurrentMarketData(tradingPair, exchangeName);

            if (marketData != null && marketData.getClosePrice() != null) {
                lastPricesCache.put(cacheKey, marketData.getClosePrice());
                return marketData.getClosePrice();
            }

            return cachedPrice; // Возвращаем кешированную цену если свежие данные недоступны

        } catch (Exception e) {
            log.error("Failed to get current price for {}: {}", tradingPair, e.getMessage());
            return null;
        }
    }

    /**
     * Обновить P&L позиции
     *
     * @param position позиция
     */
    private void updatePositionPnL(Position position) {
        if (position.getCurrentPrice() == null || position.getEntryPrice() == null) {
            return;
        }

        BigDecimal currentValue = position.getSize().multiply(position.getCurrentPrice());
        position.setCurrentValue(currentValue);

        // Рассчитываем нереализованный P&L
        int sideMultiplier = position.getSide() == OrderSide.BUY ? 1 : -1;
        BigDecimal priceDiff = position.getCurrentPrice().subtract(position.getEntryPrice());
        BigDecimal unrealizedPnl = position.getSize().multiply(priceDiff).multiply(new BigDecimal(sideMultiplier));

        position.setUnrealizedPnl(unrealizedPnl);

        // P&L в процентах
        if (position.getEntryValue().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal pnlPercent = unrealizedPnl.divide(position.getEntryValue(), 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(new BigDecimal("100"));
            position.setUnrealizedPnlPercent(pnlPercent);
        }
    }

    /**
     * Мониторинг просроченных позиций - выполняется каждую минуту
     */
    @Scheduled(fixedRate = 60000) // Каждую минуту
    public void monitorExpiredPositions() {
        try {
            LocalDateTime now = DateUtils.nowMoscow();
            List<Position> expiredPositions = positionRepository.findExpiredPositions(now);

            if (!expiredPositions.isEmpty()) {
                log.info("Found {} expired positions that need to be closed", expiredPositions.size());

                for (Position position : expiredPositions) {
                    try {
                        // Получаем текущую цену для закрытия
                        BigDecimal currentPrice = getCurrentPrice(position.getTradingPair(), position.getExchangeName());

                        if (currentPrice != null) {
                            closePosition(position, currentPrice, "Position time limit exceeded");
                        } else {
                            log.warn("Cannot close expired position {} - no current price available", position.getId());
                        }

                    } catch (Exception e) {
                        log.error("Failed to close expired position {}: {}", position.getId(), e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error monitoring expired positions: {}", e.getMessage());
        }
    }

    /**
     * Получить статистику позиций
     *
     * @return статистика позиций
     */
    public PositionStatistics getPositionStatistics() {
        try {
            long totalActivePositions = positionRepository.countActivePositions();
            BigDecimal totalUnrealizedPnl = positionRepository.calculateTotalUnrealizedPnl();
            BigDecimal totalExposure = positionRepository.calculateTotalExposure();

            // Статистика по направлениям
            List<Object[]> positionsBySide = positionRepository.getActivePositionCountBySide();
            Map<String, Integer> sideDistribution = positionsBySide.stream()
                    .collect(Collectors.toMap(
                            row -> row[0].toString(),
                            row -> ((Number) row[1]).intValue()
                    ));

            // Топ прибыльных позиций
            List<Position> topProfitable = positionRepository.findProfitableActivePositions()
                    .stream().limit(5).collect(Collectors.toList());

            return PositionStatistics.builder()
                    .totalActivePositions((int) totalActivePositions)
                    .totalUnrealizedPnl(totalUnrealizedPnl)
                    .totalExposure(totalExposure)
                    .longPositions(sideDistribution.getOrDefault("BUY", 0))
                    .shortPositions(sideDistribution.getOrDefault("SELL", 0))
                    .topProfitablePositions(topProfitable)
                    .build();

        } catch (Exception e) {
            log.error("Failed to get position statistics: {}", e.getMessage());
            return PositionStatistics.builder().build();
        }
    }

    /**
     * Закрыть все позиции (аварийная функция)
     *
     * @param reason причина закрытия
     * @return количество закрытых позиций
     */
    public int closeAllPositions(String reason) {
        try {
            List<Position> activePositions = getActivePositions();
            int closedCount = 0;

            log.warn("Closing all {} active positions: {}", activePositions.size(), reason);

            for (Position position : activePositions) {
                try {
                    BigDecimal currentPrice = getCurrentPrice(position.getTradingPair(), position.getExchangeName());

                    if (currentPrice != null) {
                        closePosition(position, currentPrice, reason);
                        closedCount++;
                    }

                } catch (Exception e) {
                    log.error("Failed to close position {} during emergency closure: {}", position.getId(), e.getMessage());
                }
            }

            // Очищаем кеш
            activePositionsCache.clear();

            log.warn("Emergency closure completed: {} of {} positions closed", closedCount, activePositions.size());
            return closedCount;

        } catch (Exception e) {
            log.error("Failed to close all positions: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Обновить кеш активных позиций
     */
    @Scheduled(fixedRate = 300000) // Каждые 5 минут
    public void refreshActivePositionsCache() {
        try {
            List<Position> activePositions = positionRepository.findByIsActiveTrueOrderByOpenedAtDesc();

            // Очищаем старый кеш
            activePositionsCache.clear();

            // Заполняем новыми данными
            for (Position position : activePositions) {
                activePositionsCache.put(position.getTradingPair(), position);
            }

            log.debug("Refreshed active positions cache: {} positions", activePositions.size());

        } catch (Exception e) {
            log.error("Failed to refresh positions cache: {}", e.getMessage());
        }
    }

    // === Вложенные классы ===

    /**
     * Статистика позиций
     */
    @lombok.Data
    @lombok.Builder
    public static class PositionStatistics {
        private int totalActivePositions;
        private BigDecimal totalUnrealizedPnl;
        private BigDecimal totalExposure;
        private int longPositions;
        private int shortPositions;
        private List<Position> topProfitablePositions;
    }
}