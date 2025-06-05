package com.example.scalpingBot.entity;

import com.example.scalpingBot.enums.TradingPairType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Сущность торговой пары для скальпинг-бота
 *
 * Хранит конфигурацию и метаданные торговых пар:
 * - Параметры торговли (минимальные объемы, точность цен)
 * - Лимиты и ограничения биржи
 * - Настройки для скальпинг-стратегии
 * - Статистика и производительность
 * - Статус активности и доступности
 *
 * Используется для:
 * - Валидации параметров ордеров
 * - Настройки алгоритмов под каждую пару
 * - Контроля доступности торговых инструментов
 * - Оптимизации параметров скальпинга
 * - Анализа производительности по парам
 *
 * @author ScalpingBot Team
 * @version 1.0
 */
@Entity
@Table(name = "trading_pairs", indexes = {
        @Index(name = "idx_trading_pair_symbol", columnList = "symbol"),
        @Index(name = "idx_trading_pair_active", columnList = "isActive"),
        @Index(name = "idx_trading_pair_scalping", columnList = "scalpingEnabled"),
        @Index(name = "idx_trading_pair_exchange", columnList = "exchangeName"),
        @Index(name = "idx_trading_pair_type", columnList = "pairType"),
        @Index(name = "idx_trading_pair_priority", columnList = "scalpingPriority")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_trading_pair_symbol_exchange",
                columnNames = {"symbol", "exchangeName"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradingPair {

    /**
     * Уникальный идентификатор
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Символ торговой пары (BTCUSDT, ETHUSDT, etc.)
     */
    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    /**
     * Базовый актив (BTC, ETH, ADA, etc.)
     */
    @Column(name = "base_asset", nullable = false, length = 10)
    private String baseAsset;

    /**
     * Котируемый актив (USDT, BTC, ETH, etc.)
     */
    @Column(name = "quote_asset", nullable = false, length = 10)
    private String quoteAsset;

    /**
     * Тип торговой пары
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "pair_type", nullable = false, length = 20)
    private TradingPairType pairType;

    /**
     * Название биржи
     */
    @Column(name = "exchange_name", nullable = false, length = 20)
    private String exchangeName;

    /**
     * Активна ли торговая пара
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    /**
     * Доступна ли для торговли
     */
    @Column(name = "is_trading_enabled", nullable = false)
    private Boolean isTradingEnabled;

    /**
     * Включен ли скальпинг для этой пары
     */
    @Column(name = "scalping_enabled", nullable = false)
    private Boolean scalpingEnabled;

    /**
     * Приоритет для скальпинга (1 = высший)
     */
    @Column(name = "scalping_priority", nullable = false)
    private Integer scalpingPriority;

    // === Параметры торговли ===

    /**
     * Минимальное количество для ордера
     */
    @Column(name = "min_quantity", nullable = false, precision = 18, scale = 8)
    private BigDecimal minQuantity;

    /**
     * Максимальное количество для ордера
     */
    @Column(name = "max_quantity", precision = 18, scale = 8)
    private BigDecimal maxQuantity;

    /**
     * Шаг количества (lot size)
     */
    @Column(name = "quantity_step", nullable = false, precision = 18, scale = 8)
    private BigDecimal quantityStep;

    /**
     * Минимальная цена
     */
    @Column(name = "min_price", nullable = false, precision = 18, scale = 8)
    private BigDecimal minPrice;

    /**
     * Максимальная цена
     */
    @Column(name = "max_price", precision = 18, scale = 8)
    private BigDecimal maxPrice;

    /**
     * Шаг цены (tick size)
     */
    @Column(name = "price_step", nullable = false, precision = 18, scale = 8)
    private BigDecimal priceStep;

    /**
     * Минимальный объем сделки в котируемой валюте
     */
    @Column(name = "min_notional", nullable = false, precision = 18, scale = 2)
    private BigDecimal minNotional;

    /**
     * Максимальный объем сделки в котируемой валюте
     */
    @Column(name = "max_notional", precision = 18, scale = 2)
    private BigDecimal maxNotional;

    /**
     * Точность цены (количество знаков после запятой)
     */
    @Column(name = "price_precision", nullable = false)
    private Integer pricePrecision;

    /**
     * Точность количества (количество знаков после запятой)
     */
    @Column(name = "quantity_precision", nullable = false)
    private Integer quantityPrecision;

    // === Настройки скальпинга ===

    /**
     * Целевая прибыль для скальпинга в процентах
     */
    @Column(name = "target_profit_percent", precision = 8, scale = 4)
    private BigDecimal targetProfitPercent;

    /**
     * Стоп-лосс для скальпинга в процентах
     */
    @Column(name = "stop_loss_percent", precision = 8, scale = 4)
    private BigDecimal stopLossPercent;

    /**
     * Максимальный спред для входа в процентах
     */
    @Column(name = "max_spread_percent", precision = 8, scale = 4)
    private BigDecimal maxSpreadPercent;

    /**
     * Минимальный объем 24ч для скальпинга
     */
    @Column(name = "min_volume_24h", precision = 18, scale = 2)
    private BigDecimal minVolume24h;

    /**
     * Максимальная волатильность (ATR) для скальпинга
     */
    @Column(name = "max_volatility_percent", precision = 8, scale = 4)
    private BigDecimal maxVolatilityPercent;

    /**
     * Интервал анализа в секундах для этой пары
     */
    @Column(name = "analysis_interval_seconds")
    private Integer analysisIntervalSeconds;

    /**
     * Максимальное время удержания позиции в минутах
     */
    @Column(name = "max_position_time_minutes")
    private Integer maxPositionTimeMinutes;

    // === Комиссии ===

    /**
     * Комиссия мейкера в процентах
     */
    @Column(name = "maker_fee_percent", precision = 8, scale = 6)
    private BigDecimal makerFeePercent;

    /**
     * Комиссия тейкера в процентах
     */
    @Column(name = "taker_fee_percent", precision = 8, scale = 6)
    private BigDecimal takerFeePercent;

    // === Статистика ===

    /**
     * Общее количество успешных сделок
     */
    @Column(name = "total_trades")
    private Integer totalTrades;

    /**
     * Количество прибыльных сделок
     */
    @Column(name = "profitable_trades")
    private Integer profitableTrades;

    /**
     * Общая прибыль по паре
     */
    @Column(name = "total_profit", precision = 18, scale = 2)
    private BigDecimal totalProfit;

    /**
     * Средняя прибыль на сделку
     */
    @Column(name = "avg_profit_per_trade", precision = 18, scale = 2)
    private BigDecimal avgProfitPerTrade;

    /**
     * Максимальная просадка по паре
     */
    @Column(name = "max_drawdown_percent", precision = 8, scale = 4)
    private BigDecimal maxDrawdownPercent;

    /**
     * Коэффициент Шарпа
     */
    @Column(name = "sharpe_ratio", precision = 8, scale = 4)
    private BigDecimal sharpeRatio;

    /**
     * Процент выигрышных сделок
     */
    @Column(name = "win_rate_percent", precision = 8, scale = 4)
    private BigDecimal winRatePercent;

    /**
     * Средний спред за последние 24 часа
     */
    @Column(name = "avg_spread_24h", precision = 8, scale = 4)
    private BigDecimal avgSpread24h;

    /**
     * Средняя волатильность за последние 24 часа
     */
    @Column(name = "avg_volatility_24h", precision = 8, scale = 4)
    private BigDecimal avgVolatility24h;

    // === Временные метки ===

    /**
     * Время создания записи
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Время последнего обновления
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Время последней сделки
     */
    @Column(name = "last_trade_at")
    private LocalDateTime lastTradeAt;

    /**
     * Время последнего обновления статистики
     */
    @Column(name = "stats_updated_at")
    private LocalDateTime statsUpdatedAt;

    /**
     * Время последней проверки активности
     */
    @Column(name = "last_health_check_at")
    private LocalDateTime lastHealthCheckAt;

    // === Дополнительная информация ===

    /**
     * Причина отключения (если неактивна)
     */
    @Column(name = "deactivation_reason", length = 200)
    private String deactivationReason;

    /**
     * Дополнительные настройки в JSON формате
     */
    @Column(name = "settings", columnDefinition = "TEXT")
    private String settings;

    /**
     * Заметки администратора
     */
    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    /**
     * Версия для оптимистичной блокировки
     */
    @Version
    private Long version;

    /**
     * Автоматические обновления временных меток
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Инициализация при создании
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        // Значения по умолчанию
        if (this.isActive == null) {
            this.isActive = true;
        }
        if (this.isTradingEnabled == null) {
            this.isTradingEnabled = true;
        }
        if (this.scalpingEnabled == null) {
            this.scalpingEnabled = false; // По умолчанию отключен
        }
        if (this.scalpingPriority == null) {
            this.scalpingPriority = 10; // Низкий приоритет по умолчанию
        }
        if (this.totalTrades == null) {
            this.totalTrades = 0;
        }
        if (this.profitableTrades == null) {
            this.profitableTrades = 0;
        }

        // Парсим базовый и котируемый активы из символа
        if (this.baseAsset == null || this.quoteAsset == null) {
            parseAssetsFromSymbol();
        }
    }

    /**
     * Парсит базовый и котируемый активы из символа
     */
    private void parseAssetsFromSymbol() {
        if (symbol == null) {
            return;
        }

        String upperSymbol = symbol.toUpperCase();

        // Определяем тип пары по символу
        if (this.pairType == null) {
            this.pairType = TradingPairType.fromPairName(upperSymbol);
        }

        // Извлекаем активы
        this.quoteAsset = this.pairType.getQuoteCurrency();
        this.baseAsset = this.pairType.getBaseCurrency(upperSymbol);
    }

    /**
     * Проверить, доступна ли пара для скальпинга
     */
    public boolean isAvailableForScalping() {
        return isActive && isTradingEnabled && scalpingEnabled;
    }

    /**
     * Проверить, подходят ли параметры ордера
     */
    public boolean isValidOrderParams(BigDecimal quantity, BigDecimal price) {
        // Проверка количества
        if (quantity.compareTo(minQuantity) < 0) {
            return false;
        }
        if (maxQuantity != null && quantity.compareTo(maxQuantity) > 0) {
            return false;
        }

        // Проверка цены
        if (price.compareTo(minPrice) < 0) {
            return false;
        }
        if (maxPrice != null && price.compareTo(maxPrice) > 0) {
            return false;
        }

        // Проверка минимального объема
        BigDecimal notional = quantity.multiply(price);
        return notional.compareTo(minNotional) >= 0 &&
                (maxNotional == null || notional.compareTo(maxNotional) <= 0);
    }

    /**
     * Округлить количество до допустимого значения
     */
    public BigDecimal roundQuantity(BigDecimal quantity) {
        if (quantityStep == null || quantityStep.compareTo(BigDecimal.ZERO) <= 0) {
            return quantity.setScale(quantityPrecision, BigDecimal.ROUND_DOWN);
        }

        BigDecimal divided = quantity.divide(quantityStep, 0, BigDecimal.ROUND_DOWN);
        return divided.multiply(quantityStep);
    }

    /**
     * Округлить цену до допустимого значения
     */
    public BigDecimal roundPrice(BigDecimal price) {
        if (priceStep == null || priceStep.compareTo(BigDecimal.ZERO) <= 0) {
            return price.setScale(pricePrecision, BigDecimal.ROUND_HALF_UP);
        }

        BigDecimal divided = price.divide(priceStep, 0, BigDecimal.ROUND_HALF_UP);
        return divided.multiply(priceStep);
    }

    /**
     * Получить минимальное количество для заданной цены
     */
    public BigDecimal getMinQuantityForPrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return minQuantity;
        }

        BigDecimal minQtyForNotional = minNotional.divide(price, quantityPrecision + 2, BigDecimal.ROUND_UP);
        return minQtyForNotional.max(minQuantity);
    }

    /**
     * Рассчитать процент выигрышных сделок
     */
    public BigDecimal calculateWinRate() {
        if (totalTrades == null || totalTrades == 0) {
            return BigDecimal.ZERO;
        }

        int profitable = profitableTrades != null ? profitableTrades : 0;
        return new BigDecimal(profitable).divide(new BigDecimal(totalTrades), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    /**
     * Обновить статистику торговли
     */
    public void updateTradingStats(boolean isProfitable, BigDecimal pnl) {
        this.totalTrades = (this.totalTrades != null ? this.totalTrades : 0) + 1;

        if (isProfitable) {
            this.profitableTrades = (this.profitableTrades != null ? this.profitableTrades : 0) + 1;
        }

        if (pnl != null) {
            this.totalProfit = (this.totalProfit != null ? this.totalProfit : BigDecimal.ZERO).add(pnl);
            this.avgProfitPerTrade = this.totalProfit.divide(new BigDecimal(this.totalTrades), 2, BigDecimal.ROUND_HALF_UP);
        }

        this.winRatePercent = calculateWinRate();
        this.lastTradeAt = LocalDateTime.now();
        this.statsUpdatedAt = LocalDateTime.now();
    }

    /**
     * Отключить скальпинг с указанием причины
     */
    public void disableScalping(String reason) {
        this.scalpingEnabled = false;
        this.deactivationReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Включить скальпинг
     */
    public void enableScalping() {
        this.scalpingEnabled = true;
        this.deactivationReason = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Проверить, активна ли пара в последние N часов
     */
    public boolean isActiveInLastHours(int hours) {
        if (lastTradeAt == null) {
            return false;
        }

        LocalDateTime threshold = LocalDateTime.now().minusHours(hours);
        return lastTradeAt.isAfter(threshold);
    }

    /**
     * Получить эффективную комиссию для скальпинга
     */
    public BigDecimal getEffectiveFeePercent() {
        // Для скальпинга обычно используется taker комиссия
        return takerFeePercent != null ? takerFeePercent : BigDecimal.ZERO;
    }

    /**
     * Получить минимальную прибыль для покрытия комиссий
     */
    public BigDecimal getMinProfitToCoverFees() {
        BigDecimal effectiveFee = getEffectiveFeePercent();
        // Комиссия на вход + комиссия на выход + минимальная прибыль
        return effectiveFee.multiply(new BigDecimal("2")).add(new BigDecimal("0.05"));
    }

    /**
     * Проверить здоровье торговой пары
     */
    public boolean isHealthy() {
        // Пара считается здоровой если:
        // 1. Активна и включена для торговли
        // 2. Есть недавняя активность (в последние 24 часа)
        // 3. Статистика не показывает критических проблем

        boolean basicHealth = isActive && isTradingEnabled;
        boolean recentActivity = isActiveInLastHours(24);
        boolean goodStats = winRatePercent == null || winRatePercent.compareTo(new BigDecimal("10")) >= 0;

        return basicHealth && (recentActivity || totalTrades == 0) && goodStats;
    }

    /**
     * Получить рекомендуемые параметры скальпинга
     */
    public String getScalpingRecommendations() {
        StringBuilder recommendations = new StringBuilder();

        if (targetProfitPercent != null && stopLossPercent != null) {
            BigDecimal ratio = targetProfitPercent.divide(stopLossPercent, 2, BigDecimal.ROUND_HALF_UP);
            recommendations.append("Risk/Reward: 1:").append(ratio).append("; ");
        }

        if (maxSpreadPercent != null) {
            recommendations.append("Max spread: ").append(maxSpreadPercent).append("%; ");
        }

        if (analysisIntervalSeconds != null) {
            recommendations.append("Analysis: every ").append(analysisIntervalSeconds).append("s; ");
        }

        return recommendations.toString();
    }

    @Override
    public String toString() {
        return String.format("TradingPair{symbol='%s', exchange='%s', active=%s, scalping=%s, priority=%d, trades=%d, winRate=%.2f%%}",
                symbol, exchangeName, isActive, scalpingEnabled, scalpingPriority,
                totalTrades != null ? totalTrades : 0,
                winRatePercent != null ? winRatePercent : BigDecimal.ZERO);
    }
}