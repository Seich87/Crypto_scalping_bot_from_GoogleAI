package com.example.scalpingBot.enums;

import lombok.Getter;
import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Перечисление типов торговых пар для скальпинг-бота
 *
 * Классификация по базовым и котируемым активам:
 * - CRYPTO_USDT - криптовалюта к USDT (основной тип для скальпинга)
 * - CRYPTO_BTC - криптовалюта к Bitcoin
 * - CRYPTO_ETH - криптовалюта к Ethereum
 * - CRYPTO_BNB - криптовалюта к Binance Coin
 * - FIAT_CRYPTO - фиатные валюты к крипте
 * - STABLECOIN_PAIR - торговля между стейблкоинами
 *
 * @author ScalpingBot Team
 * @version 1.0
 */
@Getter
public enum TradingPairType {

    /**
     * Криптовалюта к USDT - основной тип для скальпинга
     *
     * Особенности:
     * - Высокая ликвидность
     * - Низкие спреды
     * - Стабильная котировка в долларах
     * - Подходит для точного расчета P&L
     * - Оптимален для скальпинг стратегии
     *
     * Примеры: BTCUSDT, ETHUSDT, ADAUSDT
     */
    CRYPTO_USDT(
            "CRYPTO_USDT",
            "Криптовалюта к USDT",
            Pattern.compile("^[A-Z0-9]{2,10}USDT$"),
            "USDT",
            true,   // высокая ликвидность
            true,   // подходит для скальпинга
            new BigDecimal("0.05"), // минимальный спред 0.05%
            new BigDecimal("0.1"),  // комиссия 0.1%
            8,      // точность цены
            6,      // точность количества
            new BigDecimal("10.0"), // минимальный объем $10
            new BigDecimal("1000000.0"), // максимальный объем $1M
            "💰",   // эмодзи доллара
            "#00FF00", // зеленый
            1       // высший приоритет
    ),

    /**
     * Криптовалюта к Bitcoin - альтернативный тип
     *
     * Особенности:
     * - Средняя ликвидность
     * - Волатильность Bitcoin влияет на котировку
     * - Требует конвертации для расчета P&L в долларах
     * - Подходит для арбитража
     *
     * Примеры: ETHBTC, ADABTC, LINKBTC
     */
    CRYPTO_BTC(
            "CRYPTO_BTC",
            "Криптовалюта к Bitcoin",
            Pattern.compile("^[A-Z0-9]{2,10}BTC$"),
            "BTC",
            true,   // высокая ликвидность
            false,  // менее подходит для скальпинга
            new BigDecimal("0.1"),  // минимальный спред 0.1%
            new BigDecimal("0.1"),  // комиссия 0.1%
            8,      // точность цены
            6,      // точность количества
            new BigDecimal("0.0001"), // минимальный объем 0.0001 BTC
            new BigDecimal("100.0"),  // максимальный объем 100 BTC
            "₿",    // символ Bitcoin
            "#FF9500", // оранжевый Bitcoin
            3       // средний приоритет
    ),

    /**
     * Криптовалюта к Ethereum
     *
     * Особенности:
     * - Средняя ликвидность
     * - Зависимость от цены Ethereum
     * - Популярность в DeFi экосистеме
     * - Ограниченное применение в скальпинге
     *
     * Примеры: ADAETH, LINKETH, DOTETH
     */
    CRYPTO_ETH(
            "CRYPTO_ETH",
            "Криптовалюта к Ethereum",
            Pattern.compile("^[A-Z0-9]{2,10}ETH$"),
            "ETH",
            false,  // средняя ликвидность
            false,  // менее подходит для скальпинга
            new BigDecimal("0.15"), // минимальный спред 0.15%
            new BigDecimal("0.1"),  // комиссия 0.1%
            8,      // точность цены
            6,      // точность количества
            new BigDecimal("0.001"), // минимальный объем 0.001 ETH
            new BigDecimal("1000.0"), // максимальный объем 1000 ETH
            "Ξ",    // символ Ethereum
            "#627EEA", // фиолетовый Ethereum
            4       // низкий приоритет
    ),

    /**
     * Криптовалюта к Binance Coin
     *
     * Особенности:
     * - Скидка на комиссии при использовании BNB
     * - Ограниченная ликвидность
     * - Специфично для Binance экосистемы
     * - Редко используется в скальпинге
     *
     * Примеры: ETHBNB, ADABNB, DOTBNB
     */
    CRYPTO_BNB(
            "CRYPTO_BNB",
            "Криптовалюта к Binance Coin",
            Pattern.compile("^[A-Z0-9]{2,10}BNB$"),
            "BNB",
            false,  // средняя ликвидность
            false,  // не подходит для скальпинга
            new BigDecimal("0.2"),  // минимальный спред 0.2%
            new BigDecimal("0.075"), // комиссия 0.075% (скидка BNB)
            8,      // точность цены
            6,      // точность количества
            new BigDecimal("0.01"), // минимальный объем 0.01 BNB
            new BigDecimal("10000.0"), // максимальный объем 10000 BNB
            "🔸",   // ромб
            "#F3BA2F", // желтый Binance
            5       // очень низкий приоритет
    ),

    /**
     * Фиатные валюты к криптовалютам
     *
     * Особенности:
     * - Регулятивные ограничения
     * - Низкая ликвидность
     * - Высокие спреды
     * - Не подходит для скальпинга
     * - Требует KYC/AML проверки
     *
     * Примеры: BTCEUR, ETHGBP, BTCRUB
     */
    FIAT_CRYPTO(
            "FIAT_CRYPTO",
            "Фиатная валюта к криптовалюте",
            Pattern.compile("^[A-Z0-9]{2,10}(EUR|USD|GBP|RUB|JPY)$"),
            "FIAT",
            false,  // низкая ликвидность
            false,  // не подходит для скальпинга
            new BigDecimal("0.5"),  // минимальный спред 0.5%
            new BigDecimal("0.1"),  // комиссия 0.1%
            2,      // точность цены (фиат)
            6,      // точность количества
            new BigDecimal("10.0"), // минимальный объем $10
            new BigDecimal("100000.0"), // максимальный объем $100K
            "💵",   // доллар
            "#85BB65", // зеленый доллар
            6       // минимальный приоритет
    ),

    /**
     * Торговые пары между стейблкоинами
     *
     * Особенности:
     * - Очень низкая волатильность
     * - Арбитражные возможности
     * - Минимальные ценовые движения
     * - Не подходит для обычного скальпинга
     * - Специальные стратегии арбитража
     *
     * Примеры: USDCUSDT, BUSDUSDT, DAIUSDT
     */
    STABLECOIN_PAIR(
            "STABLECOIN_PAIR",
            "Стейблкоин к стейблкоину",
            Pattern.compile("^(USDC|BUSD|DAI|TUSD|PAX)(USDT|USDC|BUSD)$"),
            "STABLE",
            true,   // высокая ликвидность
            false,  // не подходит для обычного скальпинга
            new BigDecimal("0.01"), // минимальный спред 0.01%
            new BigDecimal("0.1"),  // комиссия 0.1%
            4,      // точность цены
            2,      // точность количества
            new BigDecimal("100.0"), // минимальный объем $100
            new BigDecimal("10000000.0"), // максимальный объем $10M
            "🔄",   // стрелки
            "#808080", // серый
            2       // средний приоритет для арбитража
    );

    /**
     * Код типа торговой пары
     */
    private final String code;

    /**
     * Человекочитаемое описание
     */
    private final String description;

    /**
     * Регулярное выражение для определения типа пары
     */
    private final Pattern pattern;

    /**
     * Котируемая валюта
     */
    private final String quoteCurrency;

    /**
     * Высокая ли ликвидность
     */
    private final boolean highLiquidity;

    /**
     * Подходит ли для скальпинга
     */
    private final boolean suitableForScalping;

    /**
     * Минимальный ожидаемый спред в процентах
     */
    private final BigDecimal minSpreadPercent;

    /**
     * Типичная комиссия торговли в процентах
     */
    private final BigDecimal tradingFeePercent;

    /**
     * Точность цены (количество знаков после запятой)
     */
    private final int pricePrecision;

    /**
     * Точность количества (количество знаков после запятой)
     */
    private final int quantityPrecision;

    /**
     * Минимальный объем сделки
     */
    private final BigDecimal minTradeVolume;

    /**
     * Максимальный объем сделки
     */
    private final BigDecimal maxTradeVolume;

    /**
     * Эмодзи индикатор
     */
    private final String emoji;

    /**
     * Цветовой код
     */
    private final String colorCode;

    /**
     * Приоритет для скальпинга (чем меньше число, тем выше приоритет)
     */
    private final int scalpingPriority;

    /**
     * Конструктор перечисления
     */
    TradingPairType(String code, String description, Pattern pattern, String quoteCurrency,
                    boolean highLiquidity, boolean suitableForScalping,
                    BigDecimal minSpreadPercent, BigDecimal tradingFeePercent,
                    int pricePrecision, int quantityPrecision,
                    BigDecimal minTradeVolume, BigDecimal maxTradeVolume,
                    String emoji, String colorCode, int scalpingPriority) {
        this.code = code;
        this.description = description;
        this.pattern = pattern;
        this.quoteCurrency = quoteCurrency;
        this.highLiquidity = highLiquidity;
        this.suitableForScalping = suitableForScalping;
        this.minSpreadPercent = minSpreadPercent;
        this.tradingFeePercent = tradingFeePercent;
        this.pricePrecision = pricePrecision;
        this.quantityPrecision = quantityPrecision;
        this.minTradeVolume = minTradeVolume;
        this.maxTradeVolume = maxTradeVolume;
        this.emoji = emoji;
        this.colorCode = colorCode;
        this.scalpingPriority = scalpingPriority;
    }

    /**
     * Определить тип торговой пары по названию
     *
     * @param pairName название торговой пары
     * @return тип торговой пары
     */
    public static TradingPairType fromPairName(String pairName) {
        if (pairName == null || pairName.trim().isEmpty()) {
            throw new IllegalArgumentException("Trading pair name cannot be null or empty");
        }

        String normalizedName = pairName.trim().toUpperCase();

        for (TradingPairType type : values()) {
            if (type.pattern.matcher(normalizedName).matches()) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unknown trading pair type for: " + pairName);
    }

    /**
     * Получить все типы, подходящие для скальпинга
     *
     * @return список типов для скальпинга
     */
    public static List<TradingPairType> getScalpingSuitableTypes() {
        return List.of(CRYPTO_USDT, STABLECOIN_PAIR);
    }

    /**
     * Получить рекомендуемые торговые пары для скальпинга
     *
     * @return список рекомендуемых пар
     */
    public static List<String> getRecommendedScalpingPairs() {
        return List.of(
                "BTCUSDT", "ETHUSDT", "ADAUSDT", "DOTUSDT",
                "LINKUSDT", "BNBUSDT", "SOLUSDT", "AVAXUSDT",
                "MATICUSDT", "ATOMUSDT", "LTCUSDT", "XRPUSDT"
        );
    }

    /**
     * Извлечь базовую валюту из названия пары
     *
     * @param pairName название торговой пары
     * @return базовая валюта
     */
    public String getBaseCurrency(String pairName) {
        if (pairName == null || !pattern.matcher(pairName.toUpperCase()).matches()) {
            throw new IllegalArgumentException("Invalid pair name for this type: " + pairName);
        }

        String normalizedName = pairName.toUpperCase();
        return normalizedName.replace(quoteCurrency, "");
    }

    /**
     * Проверить совместимость с уровнем риска
     *
     * @param riskLevel уровень риска
     * @return true если совместимо
     */
    public boolean isCompatibleWithRiskLevel(RiskLevel riskLevel) {
        switch (riskLevel) {
            case VERY_LOW:
            case LOW:
                return this == CRYPTO_USDT && highLiquidity;
            case MEDIUM:
                return suitableForScalping;
            case HIGH:
            case VERY_HIGH:
                return highLiquidity; // любой с высокой ликвидностью
            case CRITICAL:
                return false; // никакой тип не подходит
            default:
                return false;
        }
    }

    /**
     * Рассчитать минимальную прибыль для покрытия комиссий
     *
     * @return минимальная прибыль в процентах
     */
    public BigDecimal getMinProfitToCoverFees() {
        // Комиссия на вход + комиссия на выход + минимальная прибыль
        return tradingFeePercent.multiply(new BigDecimal("2")).add(new BigDecimal("0.1"));
    }

    /**
     * Проверить, подходит ли размер позиции для данного типа пары
     *
     * @param positionSize размер позиции
     * @return true если размер подходит
     */
    public boolean isValidPositionSize(BigDecimal positionSize) {
        return positionSize.compareTo(minTradeVolume) >= 0 &&
                positionSize.compareTo(maxTradeVolume) <= 0;
    }

    /**
     * Получить рекомендуемый интервал анализа для данного типа пары
     *
     * @return интервал в секундах
     */
    public int getRecommendedAnalysisInterval() {
        if (suitableForScalping && highLiquidity) {
            return 15; // быстрый анализ для скальпинга
        } else if (highLiquidity) {
            return 30; // средний интервал
        } else {
            return 60; // медленный анализ для низколиквидных пар
        }
    }

    /**
     * Получить максимальный рекомендуемый спред для входа в позицию
     *
     * @return максимальный спред в процентах
     */
    public BigDecimal getMaxRecommendedSpread() {
        return minSpreadPercent.multiply(new BigDecimal("3")); // в 3 раза больше минимального
    }

    /**
     * Форматированное представление для логов
     *
     * @return строковое представление с эмодзи
     */
    public String toLogString() {
        return String.format("%s %s", emoji, description);
    }

    @Override
    public String toString() {
        return description;
    }
}