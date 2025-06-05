package com.example.scalpingBot.service;

import com.example.scalpingBot.config.TradingConfig;
import com.example.scalpingBot.entity.MarketData;
import com.example.scalpingBot.entity.Position;
import com.example.scalpingBot.entity.Trade;
import com.example.scalpingBot.entity.TradingPair;
import com.example.scalpingBot.enums.OrderSide;
import com.example.scalpingBot.enums.OrderStatus;
import com.example.scalpingBot.enums.OrderType;
import com.example.scalpingBot.enums.RiskLevel;
import com.example.scalpingBot.exception.TradingException;
import com.example.scalpingBot.repository.TradeRepository;
import com.example.scalpingBot.utils.DateUtils;
import com.example.scalpingBot.utils.MathUtils;
import com.example.scalpingBot.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Основной сервис для управления торговыми операциями скальпинг-бота
 *
 * Основные функции:
 * - Анализ рыночных условий и генерация торговых сигналов
 * - Размещение и управление ордерами на биржах
 * - Контроль исполнения сделок в реальном времени
 * - Интеграция с системой риск-менеджмента
 * - Мониторинг позиций и автоматическое закрытие
 * - Реализация скальпинг-стратегии (0.8% прибыль / 0.4% стоп-лосс)
 *
 * Все операции выполняются с учетом параметров риска
 * и лимитов, установленных в конфигурации.
 *
 * @author ScalpingBot Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TradingService {

    private final TradingConfig tradingConfig;
    private final TradeRepository tradeRepository;
    private final MarketDataService marketDataService;
    private final RiskManager riskManager;
    private final PositionManager positionManager;
    private final OrderExecutionService orderExecutionService;
    private final NotificationService notificationService;

    /**
     * Константы для торговли
     */
    private static final BigDecimal MIN_PROFIT_RATIO = new BigDecimal("1.5"); // Минимальное соотношение прибыль/риск
    private static final int MAX_RETRIES = 3; // Максимальное количество попыток
    private static final int RETRY_DELAY_MS = 1000; // Задержка между попытками

    /**
     * Выполнить полный цикл анализа и торговли
     * Основной метод, вызываемый планировщиком каждые 15 секунд
     */
    @Async
    public CompletableFuture<Void> executeScalpingCycle() {
        if (!tradingConfig.isTradingAllowed()) {
            log.debug("Trading is not allowed at this time");
            return CompletableFuture.completedFuture(null);
        }

        try {
            log.debug("Starting scalping cycle at {}", DateUtils.formatForLog(DateUtils.nowMoscow()));

            // 1. Проверяем общее состояние системы
            if (!riskManager.isTradingAllowed()) {
                log.info("Trading suspended by risk management");
                return CompletableFuture.completedFuture(null);
            }

            // 2. Обновляем рыночные данные
            updateMarketData();

            // 3. Мониторим существующие позиции
            monitorExistingPositions();

            // 4. Ищем новые торговые возможности
            if (riskManager.canOpenNewPosition()) {
                analyzeAndTrade();
            }

            log.debug("Scalping cycle completed successfully");

        } catch (Exception e) {
            log.error("Error in scalping cycle: {}", e.getMessage(), e);
            notificationService.sendErrorAlert("Scalping Cycle Error", e.getMessage());
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Анализировать рынок и принимать торговые решения
     */
    private void analyzeAndTrade() {
        try {
            List<String> tradingPairs = tradingConfig.getTradingPairs();

            for (String pair : tradingPairs) {
                try {
                    analyzeAndTradeSymbol(pair);
                } catch (Exception e) {
                    log.error("Error analyzing {}: {}", pair, e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error in analyze and trade: {}", e.getMessage());
        }
    }

    /**
     * Анализировать конкретную торговую пару и принять решение
     *
     * @param tradingPair торговая пара для анализа
     */
    private void analyzeAndTradeSymbol(String tradingPair) {
        log.debug("Analyzing trading opportunity for {}", tradingPair);

        try {
            // Получаем актуальные рыночные данные
            MarketData marketData = marketDataService.getCurrentMarketData(tradingPair, "binance");

            if (marketData == null || !marketData.isHighQualityData()) {
                log.debug("Poor quality market data for {}, skipping", tradingPair);
                return;
            }

            // Проверяем, есть ли уже активная позиция по этой паре
            if (positionManager.hasActivePosition(tradingPair)) {
                log.debug("Active position already exists for {}, skipping", tradingPair);
                return;
            }

            // Проверяем рыночные условия
            if (!isMarketSuitableForScalping(marketData)) {
                log.debug("Market conditions not suitable for scalping {}", tradingPair);
                return;
            }

            // Генерируем торговый сигнал
            TradingSignal signal = generateTradingSignal(marketData);

            if (signal.getStrength().abs().compareTo(new BigDecimal("0.7")) >= 0) {
                // Сильный сигнал - пытаемся открыть позицию
                executeTradeSignal(tradingPair, signal, marketData);
            }

        } catch (Exception e) {
            log.error("Error analyzing symbol {}: {}", tradingPair, e.getMessage());
        }
    }

    /**
     * Проверить, подходят ли рыночные условия для скальпинга
     *
     * @param marketData рыночные данные
     * @return true если условия подходят
     */
    private boolean isMarketSuitableForScalping(MarketData marketData) {
        // Проверяем базовые условия
        if (!marketData.isSuitableForScalping()) {
            return false;
        }

        // Проверяем волатильность (не слишком высокая и не слишком низкая)
        if (marketData.getAtrPercent() != null) {
            BigDecimal minVol = new BigDecimal("0.5");
            BigDecimal maxVol = tradingConfig.getStrategy().getMaxSpreadPercent().multiply(new BigDecimal("10"));

            if (marketData.getAtrPercent().compareTo(minVol) < 0 ||
                    marketData.getAtrPercent().compareTo(maxVol) > 0) {
                return false;
            }
        }

        // Проверяем спред
        if (marketData.getSpreadPercent() != null &&
                marketData.getSpreadPercent().compareTo(tradingConfig.getStrategy().getMaxSpreadPercent()) > 0) {
            return false;
        }

        // Проверяем объем
        if (marketData.getVolume24h() != null &&
                marketData.getVolume24h().compareTo(tradingConfig.getStrategy().getMinimumVolumeUsdt()) < 0) {
            return false;
        }

        return true;
    }

    /**
     * Генерировать торговый сигнал на основе технического анализа
     *
     * @param marketData рыночные данные
     * @return торговый сигнал
     */
    private TradingSignal generateTradingSignal(MarketData marketData) {
        BigDecimal bullishStrength = marketData.getBullishSignalStrength();
        BigDecimal bearishStrength = marketData.getBearishSignalStrength();

        // Определяем общую силу сигнала
        BigDecimal netStrength = bullishStrength.subtract(bearishStrength).divide(new BigDecimal("100"));

        // Определяем направление
        OrderSide side = netStrength.compareTo(BigDecimal.ZERO) > 0 ? OrderSide.BUY : OrderSide.SELL;

        // Определяем уровень уверенности
        BigDecimal confidence = calculateSignalConfidence(marketData);

        return TradingSignal.builder()
                .side(side)
                .strength(netStrength.abs())
                .confidence(confidence)
                .timestamp(DateUtils.nowMoscow())
                .build();
    }

    /**
     * Рассчитать уровень уверенности в сигнале
     *
     * @param marketData рыночные данные
     * @return уровень уверенности (0-1)
     */
    private BigDecimal calculateSignalConfidence(MarketData marketData) {
        BigDecimal confidence = new BigDecimal("0.5"); // Базовый уровень

        // Увеличиваем уверенность при хорошей ликвидности
        if (marketData.getLiquidityIndex() != null &&
                marketData.getLiquidityIndex().compareTo(new BigDecimal("80")) >= 0) {
            confidence = confidence.add(new BigDecimal("0.2"));
        }

        // Увеличиваем при низком спреде
        if (marketData.getSpreadPercent() != null &&
                marketData.getSpreadPercent().compareTo(new BigDecimal("0.05")) <= 0) {
            confidence = confidence.add(new BigDecimal("0.15"));
        }

        // Увеличиваем при подтверждении нескольких индикаторов
        int confirmingIndicators = countConfirmingIndicators(marketData);
        confidence = confidence.add(new BigDecimal(confirmingIndicators * 0.1));

        return MathUtils.clamp(confidence, BigDecimal.ZERO, BigDecimal.ONE);
    }

    /**
     * Подсчитать количество подтверждающих индикаторов
     *
     * @param marketData рыночные данные
     * @return количество подтверждающих индикаторов
     */
    private int countConfirmingIndicators(MarketData marketData) {
        int count = 0;

        // RSI
        if (marketData.getRsi() != null) {
            if (marketData.getRsi().compareTo(new BigDecimal("30")) <= 0 ||
                    marketData.getRsi().compareTo(new BigDecimal("70")) >= 0) {
                count++;
            }
        }

        // MACD
        if (marketData.getMacdLine() != null && marketData.getMacdSignal() != null) {
            // Проверяем пересечение MACD
            BigDecimal macdDiff = marketData.getMacdLine().subtract(marketData.getMacdSignal());
            if (macdDiff.abs().compareTo(new BigDecimal("0.1")) >= 0) {
                count++;
            }
        }

        // Bollinger Bands
        if (marketData.isBollingerBreakout()) {
            count++;
        }

        return count;
    }

    /**
     * Выполнить торговый сигнал
     *
     * @param tradingPair торговая пара
     * @param signal торговый сигнал
     * @param marketData рыночные данные
     */
    private void executeTradeSignal(String tradingPair, TradingSignal signal, MarketData marketData) {
        try {
            log.info("Executing {} signal for {} with strength {}",
                    signal.getSide(), tradingPair, signal.getStrength());

            // Рассчитываем параметры позиции
            PositionParameters params = calculatePositionParameters(tradingPair, signal, marketData);

            if (params == null) {
                log.warn("Failed to calculate position parameters for {}", tradingPair);
                return;
            }

            // Проверяем риски
            if (!riskManager.validateNewPosition(params)) {
                log.info("Position rejected by risk management: {}", tradingPair);
                return;
            }

            // Размещаем ордер
            Trade trade = placeOrder(params);

            if (trade != null) {
                log.info("Successfully placed {} order for {}: {} at {}",
                        trade.getOrderSide(), tradingPair, trade.getQuantity(), trade.getPrice());

                notificationService.sendTradeAlert("Order Placed",
                        String.format("%s %s %s at %s",
                                trade.getOrderSide(), trade.getQuantity(), tradingPair, trade.getPrice()));
            }

        } catch (Exception e) {
            log.error("Failed to execute trade signal for {}: {}", tradingPair, e.getMessage());
            notificationService.sendErrorAlert("Trade Execution Failed",
                    String.format("Failed to execute %s for %s: %s", signal.getSide(), tradingPair, e.getMessage()));
        }
    }

    /**
     * Рассчитать параметры позиции
     *
     * @param tradingPair торговая пара
     * @param signal торговый сигнал
     * @param marketData рыночные данные
     * @return параметры позиции
     */
    private PositionParameters calculatePositionParameters(String tradingPair, TradingSignal signal, MarketData marketData) {
        try {
            BigDecimal currentPrice = marketData.getClosePrice();
            BigDecimal accountBalance = riskManager.getAvailableBalance();

            // Определяем размер позиции на основе риска
            BigDecimal riskPercent = tradingConfig.getStrategy().getStopLossPercent();
            BigDecimal maxPositionPercent = riskManager.getMaxPositionSizePercent();

            // Рассчитываем цены стоп-лосса и тейк-профита
            BigDecimal stopLossPrice;
            BigDecimal takeProfitPrice;

            if (signal.getSide() == OrderSide.BUY) {
                stopLossPrice = currentPrice.multiply(
                        BigDecimal.ONE.subtract(riskPercent.divide(new BigDecimal("100")))
                );
                takeProfitPrice = currentPrice.multiply(
                        BigDecimal.ONE.add(tradingConfig.getStrategy().getTargetProfitPercent().divide(new BigDecimal("100")))
                );
            } else {
                stopLossPrice = currentPrice.multiply(
                        BigDecimal.ONE.add(riskPercent.divide(new BigDecimal("100")))
                );
                takeProfitPrice = currentPrice.multiply(
                        BigDecimal.ONE.subtract(tradingConfig.getStrategy().getTargetProfitPercent().divide(new BigDecimal("100")))
                );
            }

            // Рассчитываем количество
            BigDecimal riskAmount = MathUtils.percentageOf(accountBalance, maxPositionPercent);
            BigDecimal priceDifference = currentPrice.subtract(stopLossPrice).abs();
            BigDecimal quantity = MathUtils.safeDivide(riskAmount, priceDifference);

            // Проверяем минимальные требования
            if (quantity.compareTo(new BigDecimal("0.001")) < 0) {
                log.warn("Calculated quantity too small for {}: {}", tradingPair, quantity);
                return null;
            }

            return PositionParameters.builder()
                    .tradingPair(tradingPair)
                    .side(signal.getSide())
                    .quantity(quantity)
                    .price(currentPrice)
                    .stopLossPrice(stopLossPrice)
                    .takeProfitPrice(takeProfitPrice)
                    .riskLevel(RiskLevel.fromVolatility(marketData.getAtrPercent()))
                    .signalStrength(signal.getStrength())
                    .confidence(signal.getConfidence())
                    .build();

        } catch (Exception e) {
            log.error("Failed to calculate position parameters: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Разместить ордер на бирже
     *
     * @param params параметры позиции
     * @return созданная торговая операция
     */
    private Trade placeOrder(PositionParameters params) {
        try {
            // Валидируем параметры
            ValidationUtils.validateOrderParameters(
                    params.getTradingPair(),
                    OrderType.MARKET, // Для скальпинга используем рыночные ордера
                    params.getSide(),
                    params.getQuantity(),
                    params.getPrice()
            );

            // Создаем объект Trade
            Trade trade = Trade.builder()
                    .tradingPair(params.getTradingPair())
                    .orderType(OrderType.MARKET)
                    .orderSide(params.getSide())
                    .quantity(params.getQuantity())
                    .price(params.getPrice())
                    .stopLossPrice(params.getStopLossPrice())
                    .takeProfitPrice(params.getTakeProfitPrice())
                    .status(OrderStatus.PENDING)
                    .exchangeName("binance")
                    .isScalping(true)
                    .strategyName("ScalpingStrategy")
                    .signalStrength(params.getSignalStrength())
                    .createdAt(DateUtils.nowMoscow())
                    .build();

            // Сохраняем в БД
            trade = tradeRepository.save(trade);

            // Отправляем на исполнение
            CompletableFuture<Trade> executionResult = orderExecutionService.executeOrder(trade);

            // Асинхронно обрабатываем результат
            executionResult.thenAccept(executedTrade -> {
                if (executedTrade.getStatus() == OrderStatus.FILLED) {
                    // Создаем позицию
                    positionManager.createPositionFromTrade(executedTrade);
                } else if (executedTrade.getStatus() == OrderStatus.REJECTED) {
                    notificationService.sendErrorAlert("Order Rejected",
                            String.format("Order rejected for %s: %s",
                                    executedTrade.getTradingPair(), executedTrade.getNotes()));
                }
            });

            return trade;

        } catch (Exception e) {
            log.error("Failed to place order: {}", e.getMessage());
            throw new TradingException(TradingException.TradingErrorType.ORDER_FAILED,
                    "Failed to place order: " + e.getMessage(), params.getTradingPair(), e);
        }
    }

    /**
     * Мониторить существующие позиции
     */
    private void monitorExistingPositions() {
        try {
            List<Position> activePositions = positionManager.getActivePositions();

            if (activePositions.isEmpty()) {
                return;
            }

            log.debug("Monitoring {} active positions", activePositions.size());

            for (Position position : activePositions) {
                try {
                    monitorPosition(position);
                } catch (Exception e) {
                    log.error("Error monitoring position {}: {}", position.getId(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error monitoring positions: {}", e.getMessage());
        }
    }

    /**
     * Мониторить конкретную позицию
     *
     * @param position позиция для мониторинга
     */
    private void monitorPosition(Position position) {
        // Обновляем текущую цену
        MarketData marketData = marketDataService.getCurrentMarketData(position.getTradingPair(), position.getExchangeName());

        if (marketData != null && marketData.getClosePrice() != null) {
            position.updateCurrentPrice(marketData.getClosePrice());
        }

        // Проверяем условия закрытия
        if (shouldClosePosition(position)) {
            closePosition(position, "Condition triggered");
        }
    }

    /**
     * Проверить, нужно ли закрыть позицию
     *
     * @param position позиция
     * @return true если нужно закрыть
     */
    private boolean shouldClosePosition(Position position) {
        // Проверяем время
        if (position.isExpired()) {
            log.info("Position {} expired, closing", position.getId());
            return true;
        }

        // Проверяем стоп-лосс
        if (position.isStopLossTriggered()) {
            log.info("Stop loss triggered for position {}", position.getId());
            return true;
        }

        // Проверяем тейк-профит
        if (position.isTakeProfitTriggered()) {
            log.info("Take profit triggered for position {}", position.getId());
            return true;
        }

        // Проверяем риск-менеджмент
        if (riskManager.shouldClosePosition(position)) {
            log.info("Risk management requires closing position {}", position.getId());
            return true;
        }

        return false;
    }

    /**
     * Закрыть позицию
     *
     * @param position позиция для закрытия
     * @param reason причина закрытия
     */
    private void closePosition(Position position, String reason) {
        try {
            log.info("Closing position {} for {}: {}", position.getId(), position.getTradingPair(), reason);

            // Размещаем ордер на закрытие
            OrderSide closeSide = position.getSide().getOpposite();

            Trade closeTrade = Trade.builder()
                    .tradingPair(position.getTradingPair())
                    .orderType(OrderType.MARKET)
                    .orderSide(closeSide)
                    .quantity(position.getSize())
                    .positionId(position.getId())
                    .status(OrderStatus.PENDING)
                    .exchangeName(position.getExchangeName())
                    .isScalping(true)
                    .closeReason(reason)
                    .createdAt(DateUtils.nowMoscow())
                    .build();

            closeTrade = tradeRepository.save(closeTrade);

            // Выполняем ордер на закрытие
            orderExecutionService.executeOrder(closeTrade).thenAccept(executedTrade -> {
                if (executedTrade.getStatus() == OrderStatus.FILLED) {
                    positionManager.closePosition(position, executedTrade.getAvgPrice(), reason);

                    notificationService.sendTradeAlert("Position Closed",
                            String.format("Closed %s position in %s. P&L: %.2f",
                                    position.getSide(), position.getTradingPair(), position.getRealizedPnl()));
                }
            });

        } catch (Exception e) {
            log.error("Failed to close position {}: {}", position.getId(), e.getMessage());
            notificationService.sendErrorAlert("Position Close Failed",
                    String.format("Failed to close position %d: %s", position.getId(), e.getMessage()));
        }
    }

    /**
     * Обновить рыночные данные для всех активных пар
     */
    private void updateMarketData() {
        try {
            marketDataService.getAllMarketData("binance").thenAccept(marketDataMap -> {
                log.debug("Updated market data for {} pairs", marketDataMap.size());
            });
        } catch (Exception e) {
            log.error("Failed to update market data: {}", e.getMessage());
        }
    }

    /**
     * Остановить всю торговлю (аварийная остановка)
     *
     * @param reason причина остановки
     */
    public void emergencyStop(String reason) {
        log.error("EMERGENCY STOP TRIGGERED: {}", reason);

        try {
            // Закрываем все позиции
            List<Position> activePositions = positionManager.getActivePositions();
            for (Position position : activePositions) {
                closePosition(position, "Emergency stop: " + reason);
            }

            // Отменяем все активные ордера
            cancelAllActiveOrders();

            // Уведомляем
            notificationService.sendCriticalAlert("EMERGENCY STOP",
                    String.format("Trading stopped: %s. Closed %d positions.", reason, activePositions.size()));

        } catch (Exception e) {
            log.error("Error during emergency stop: {}", e.getMessage());
        }
    }

    /**
     * Отменить все активные ордера
     */
    private void cancelAllActiveOrders() {
        try {
            List<Trade> activeOrders = tradeRepository.findActiveOrders();

            for (Trade order : activeOrders) {
                orderExecutionService.cancelOrder(order);
            }

            log.info("Cancelled {} active orders", activeOrders.size());

        } catch (Exception e) {
            log.error("Failed to cancel active orders: {}", e.getMessage());
        }
    }

    /**
     * Получить статистику торговли за сегодня
     *
     * @return статистика торговли
     */
    public TradingStatistics getTodayStatistics() {
        try {
            List<Trade> todayTrades = tradeRepository.findTodayTrades();
            BigDecimal dailyPnl = tradeRepository.calculateDailyPnl();
            long totalTrades = tradeRepository.countTodayTrades();
            long profitableTrades = todayTrades.stream()
                    .mapToLong(t -> t.isProfitable() ? 1 : 0)
                    .sum();

            return TradingStatistics.builder()
                    .totalTrades((int) totalTrades)
                    .profitableTrades((int) profitableTrades)
                    .dailyPnl(dailyPnl)
                    .winRate(totalTrades > 0 ? (double) profitableTrades / totalTrades * 100 : 0)
                    .build();

        } catch (Exception e) {
            log.error("Failed to get trading statistics: {}", e.getMessage());
            return TradingStatistics.builder().build();
        }
    }

    // === Вложенные классы ===

    /**
     * Торговый сигнал
     */
    @lombok.Data
    @lombok.Builder
    private static class TradingSignal {
        private OrderSide side;
        private BigDecimal strength;
        private BigDecimal confidence;
        private LocalDateTime timestamp;
    }

    /**
     * Параметры позиции
     */
    @lombok.Data
    @lombok.Builder
    private static class PositionParameters {
        private String tradingPair;
        private OrderSide side;
        private BigDecimal quantity;
        private BigDecimal price;
        private BigDecimal stopLossPrice;
        private BigDecimal takeProfitPrice;
        private RiskLevel riskLevel;
        private BigDecimal signalStrength;
        private BigDecimal confidence;
    }

    /**
     * Статистика торговли
     */
    @lombok.Data
    @lombok.Builder
    public static class TradingStatistics {
        private int totalTrades;
        private int profitableTrades;
        private BigDecimal dailyPnl;
        private double winRate;
    }
}