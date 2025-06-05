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
 * Сущность рыночных данных для скальпинг-бота
 *
 * Хранит актуальную рыночную информацию для принятия торговых решений:
 * - Ценовые данные (OHLCV) в реальном времени
 * - Технические индикаторы (RSI, EMA, MACD, Bollinger Bands, ATR)
 * - Данные стакана заявок (bid/ask, спреды)
 * - Объемы торгов и ликвидность
 * - Метрики для анализа рыночных условий
 *
 * Используется для:
 * - Принятия решений о входе/выходе из позиций
 * - Расчета технических индикаторов в реальном времени
 * - Мониторинга рыночных условий (спреды, волатильность)
 * - Исторического анализа и бэктестинга
 * - Определения оптимальных моментов для скальпинга
 *
 * @author ScalpingBot Team
 * @version 1.0
 */
@Entity
@Table(name = "market_data", indexes = {
        @Index(name = "idx_market_data_pair_time", columnList = "tradingPair, timestamp"),
        @Index(name = "idx_market_data_timestamp", columnList = "timestamp"),
        @Index(name = "idx_market_data_pair", columnList = "tradingPair"),
        @Index(name = "idx_market_data_exchange", columnList = "exchangeName"),
        @Index(name = "idx_market_data_volume", columnList = "volume24h"),
        @Index(name = "idx_market_data_volatility", columnList = "atr")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_market_data_pair_time_exchange",
                columnNames = {"tradingPair", "timestamp", "exchangeName"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketData {

    /**
     * Уникальный идентификатор записи
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Торговая пара
     */
    @Column(name = "trading_pair", nullable = false, length = 20)
    private String tradingPair;

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
     * Временная метка данных
     */
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    /**
     * Временная метка биржи (Unix timestamp в миллисекундах)
     */
    @Column(name = "exchange_timestamp")
    private Long exchangeTimestamp;

    // === OHLCV данные ===

    /**
     * Цена открытия (Open)
     */
    @Column(name = "open_price", nullable = false, precision = 18, scale = 8)
    private BigDecimal openPrice;

    /**
     * Максимальная цена (High)
     */
    @Column(name = "high_price", nullable = false, precision = 18, scale = 8)
    private BigDecimal highPrice;

    /**
     * Минимальная цена (Low)
     */
    @Column(name = "low_price", nullable = false, precision = 18, scale = 8)
    private BigDecimal lowPrice;

    /**
     * Цена закрытия (Close) - текущая цена
     */
    @Column(name = "close_price", nullable = false, precision = 18, scale = 8)
    private BigDecimal closePrice;

    /**
     * Объем торгов за период
     */
    @Column(name = "volume", nullable = false, precision = 18, scale = 8)
    private BigDecimal volume;

    /**
     * Объем в котируемой валюте (обычно USDT)
     */
    @Column(name = "quote_volume", precision = 18, scale = 2)
    private BigDecimal quoteVolume;

    /**
     * Объем торгов за 24 часа
     */
    @Column(name = "volume_24h", precision = 18, scale = 8)
    private BigDecimal volume24h;

    /**
     * Объем в USDT за 24 часа
     */
    @Column(name = "quote_volume_24h", precision = 18, scale = 2)
    private BigDecimal quoteVolume24h;

    /**
     * Количество сделок за период
     */
    @Column(name = "trade_count")
    private Integer tradeCount;

    // === Данные стакана заявок ===

    /**
     * Лучшая цена покупки (bid)
     */
    @Column(name = "bid_price", precision = 18, scale = 8)
    private BigDecimal bidPrice;

    /**
     * Объем на лучшей цене покупки
     */
    @Column(name = "bid_quantity", precision = 18, scale = 8)
    private BigDecimal bidQuantity;

    /**
     * Лучшая цена продажи (ask)
     */
    @Column(name = "ask_price", precision = 18, scale = 8)
    private BigDecimal askPrice;

    /**
     * Объем на лучшей цене продажи
     */
    @Column(name = "ask_quantity", precision = 18, scale = 8)
    private BigDecimal askQuantity;

    /**
     * Спред между bid и ask в процентах
     */
    @Column(name = "spread_percent", precision = 8, scale = 4)
    private BigDecimal spreadPercent;

    /**
     * Средневзвешенная цена
     */
    @Column(name = "weighted_avg_price", precision = 18, scale = 8)
    private BigDecimal weightedAvgPrice;

    /**
     * Изменение цены за 24 часа в процентах
     */
    @Column(name = "price_change_24h_percent", precision = 8, scale = 4)
    private BigDecimal priceChange24hPercent;

    // === Технические индикаторы ===

    /**
     * RSI (Relative Strength Index) - 14 период
     */
    @Column(name = "rsi", precision = 8, scale = 4)
    private BigDecimal rsi;

    /**
     * EMA9 (Exponential Moving Average 9 periods)
     */
    @Column(name = "ema9", precision = 18, scale = 8)
    private BigDecimal ema9;

    /**
     * EMA21 (Exponential Moving Average 21 periods)
     */
    @Column(name = "ema21", precision = 18, scale = 8)
    private BigDecimal ema21;

    /**
     * MACD линия
     */
    @Column(name = "macd_line", precision = 18, scale = 8)
    private BigDecimal macdLine;

    /**
     * MACD сигнальная линия
     */
    @Column(name = "macd_signal", precision = 18, scale = 8)
    private BigDecimal macdSignal;

    /**
     * MACD гистограмма
     */
    @Column(name = "macd_histogram", precision = 18, scale = 8)
    private BigDecimal macdHistogram;

    /**
     * Bollinger Bands - верхняя полоса
     */
    @Column(name = "bb_upper", precision = 18, scale = 8)
    private BigDecimal bbUpper;

    /**
     * Bollinger Bands - средняя линия (SMA20)
     */
    @Column(name = "bb_middle", precision = 18, scale = 8)
    private BigDecimal bbMiddle;

    /**
     * Bollinger Bands - нижняя полоса
     */
    @Column(name = "bb_lower", precision = 18, scale = 8)
    private BigDecimal bbLower;

    /**
     * ATR (Average True Range) - 14 период
     */
    @Column(name = "atr", precision = 18, scale = 8)
    private BigDecimal atr;

    /**
     * ATR в процентах от цены
     */
    @Column(name = "atr_percent", precision = 8, scale = 4)
    private BigDecimal atrPercent;

    // === Дополнительные метрики ===

    /**
     * Сила тренда (-1 до 1, где -1 = сильный нисходящий, 1 = сильный восходящий)
     */
    @Column(name = "trend_strength", precision = 8, scale = 4)
    private BigDecimal trendStrength;

    /**
     * Сигнал для скальпинга (-1 до 1)
     */
    @Column(name = "scalping_signal", precision = 8, scale = 4)
    private BigDecimal scalpingSignal;

    /**
     * Индекс ликвидности (0-100)
     */
    @Column(name = "liquidity_index", precision = 8, scale = 4)
    private BigDecimal liquidityIndex;

    /**
     * Волатильность за последний час в процентах
     */
    @Column(name = "volatility_1h", precision = 8, scale = 4)
    private BigDecimal volatility1h;

    /**
     * Средний размер сделки за период
     */
    @Column(name = "avg_trade_size", precision = 18, scale = 8)
    private BigDecimal avgTradeSize;

    /**
     * Доминирование покупателей/продавцов (-1 до 1)
     */
    @Column(name = "buy_sell_ratio", precision = 8, scale = 4)
    private BigDecimal buySellRatio;

    /**
     * Качество данных (0-100, где 100 = отличное)
     */
    @Column(name = "data_quality", precision = 5, scale = 2)
    private BigDecimal dataQuality;

    /**
     * Задержка получения данных в миллисекундах
     */
    @Column(name = "latency_ms")
    private Integer latencyMs;

    /**
     * Время создания записи
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Дополнительные метаданные в JSON формате
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * Автоматическая установка времени создания
     */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }

    /**
     * Рассчитать спред между bid и ask
     */
    public void calculateSpread() {
        if (bidPrice != null && askPrice != null && bidPrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal spread = askPrice.subtract(bidPrice);
            this.spreadPercent = spread.divide(bidPrice, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
    }

    /**
     * Рассчитать ATR в процентах
     */
    public void calculateAtrPercent() {
        if (atr != null && closePrice != null && closePrice.compareTo(BigDecimal.ZERO) > 0) {
            this.atrPercent = atr.divide(closePrice, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
    }

    /**
     * Проверить, подходят ли условия для скальпинга
     */
    public boolean isSuitableForScalping() {
        // Проверяем основные условия для скальпинга
        boolean goodSpread = spreadPercent != null && spreadPercent.compareTo(new BigDecimal("0.1")) <= 0;
        boolean goodVolume = volume24h != null && volume24h.compareTo(new BigDecimal("1000000")) >= 0;
        boolean goodLiquidity = liquidityIndex != null && liquidityIndex.compareTo(new BigDecimal("70")) >= 0;
        boolean moderateVolatility = atrPercent != null &&
                atrPercent.compareTo(new BigDecimal("0.5")) >= 0 &&
                atrPercent.compareTo(new BigDecimal("5.0")) <= 0;

        return goodSpread && goodVolume && goodLiquidity && moderateVolatility;
    }

    /**
     * Получить силу бычьего сигнала (0-100)
     */
    public BigDecimal getBullishSignalStrength() {
        BigDecimal strength = BigDecimal.ZERO;
        int factors = 0;

        // RSI в зоне перепроданности
        if (rsi != null && rsi.compareTo(new BigDecimal("30")) <= 0) {
            strength = strength.add(new BigDecimal("25"));
        }
        factors++;

        // Цена выше EMA9
        if (ema9 != null && closePrice != null && closePrice.compareTo(ema9) > 0) {
            strength = strength.add(new BigDecimal("25"));
        }
        factors++;

        // MACD бычий сигнал
        if (macdLine != null && macdSignal != null && macdLine.compareTo(macdSignal) > 0) {
            strength = strength.add(new BigDecimal("25"));
        }
        factors++;

        // Цена у нижней Bollinger Band
        if (bbLower != null && closePrice != null &&
                closePrice.compareTo(bbLower.multiply(new BigDecimal("1.005"))) <= 0) {
            strength = strength.add(new BigDecimal("25"));
        }
        factors++;

        return factors > 0 ? strength : BigDecimal.ZERO;
    }

    /**
     * Получить силу медвежьего сигнала (0-100)
     */
    public BigDecimal getBearishSignalStrength() {
        BigDecimal strength = BigDecimal.ZERO;
        int factors = 0;

        // RSI в зоне перекупленности
        if (rsi != null && rsi.compareTo(new BigDecimal("70")) >= 0) {
            strength = strength.add(new BigDecimal("25"));
        }
        factors++;

        // Цена ниже EMA9
        if (ema9 != null && closePrice != null && closePrice.compareTo(ema9) < 0) {
            strength = strength.add(new BigDecimal("25"));
        }
        factors++;

        // MACD медвежий сигнал
        if (macdLine != null && macdSignal != null && macdLine.compareTo(macdSignal) < 0) {
            strength = strength.add(new BigDecimal("25"));
        }
        factors++;

        // Цена у верхней Bollinger Band
        if (bbUpper != null && closePrice != null &&
                closePrice.compareTo(bbUpper.multiply(new BigDecimal("0.995"))) >= 0) {
            strength = strength.add(new BigDecimal("25"));
        }
        factors++;

        return factors > 0 ? strength : BigDecimal.ZERO;
    }

    /**
     * Проверить, пробила ли цена Bollinger Bands
     */
    public boolean isBollingerBreakout() {
        if (closePrice == null || bbUpper == null || bbLower == null) {
            return false;
        }

        return closePrice.compareTo(bbUpper) > 0 || closePrice.compareTo(bbLower) < 0;
    }

    /**
     * Получить направление тренда
     */
    public String getTrendDirection() {
        if (trendStrength == null) {
            return "UNKNOWN";
        }

        if (trendStrength.compareTo(new BigDecimal("0.3")) > 0) {
            return "BULLISH";
        } else if (trendStrength.compareTo(new BigDecimal("-0.3")) < 0) {
            return "BEARISH";
        } else {
            return "SIDEWAYS";
        }
    }

    /**
     * Проверить, являются ли данные актуальными
     */
    public boolean isDataFresh(int maxAgeSeconds) {
        if (timestamp == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        long ageSeconds = java.time.Duration.between(timestamp, now).getSeconds();

        return ageSeconds <= maxAgeSeconds;
    }

    /**
     * Получить возраст данных в секундах
     */
    public long getDataAgeSeconds() {
        if (timestamp == null) {
            return Long.MAX_VALUE;
        }

        LocalDateTime now = LocalDateTime.now();
        return java.time.Duration.between(timestamp, now).getSeconds();
    }

    /**
     * Проверить качество данных
     */
    public boolean isHighQualityData() {
        return dataQuality != null && dataQuality.compareTo(new BigDecimal("80")) >= 0;
    }

    /**
     * Получить краткое описание рыночного состояния
     */
    public String getMarketConditionSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(getTrendDirection()).append(" trend");

        if (atrPercent != null) {
            if (atrPercent.compareTo(new BigDecimal("2")) < 0) {
                summary.append(", LOW volatility");
            } else if (atrPercent.compareTo(new BigDecimal("5")) > 0) {
                summary.append(", HIGH volatility");
            } else {
                summary.append(", NORMAL volatility");
            }
        }

        if (liquidityIndex != null) {
            if (liquidityIndex.compareTo(new BigDecimal("80")) >= 0) {
                summary.append(", EXCELLENT liquidity");
            } else if (liquidityIndex.compareTo(new BigDecimal("60")) >= 0) {
                summary.append(", GOOD liquidity");
            } else {
                summary.append(", LOW liquidity");
            }
        }

        return summary.toString();
    }

    @Override
    public String toString() {
        return String.format("MarketData{pair='%s', price=%.8f, volume24h=%.0f, rsi=%.2f, atr=%.4f%%, trend=%s, time=%s}",
                tradingPair,
                closePrice != null ? closePrice : BigDecimal.ZERO,
                volume24h != null ? volume24h : BigDecimal.ZERO,
                rsi != null ? rsi : BigDecimal.ZERO,
                atrPercent != null ? atrPercent : BigDecimal.ZERO,
                getTrendDirection(),
                timestamp);
    }
}