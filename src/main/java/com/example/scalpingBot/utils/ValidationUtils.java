package com.example.scalpingBot.utils;

import com.example.scalpingBot.enums.OrderSide;
import com.example.scalpingBot.enums.OrderType;
import com.example.scalpingBot.enums.RiskLevel;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Утилиты валидации для скальпинг-бота
 *
 * Основные функции:
 * - Валидация торговых параметров
 * - Проверка лимитов риск-менеджмента
 * - Валидация API ключей и подписей
 * - Проверка рыночных условий
 * - Валидация торговых пар и форматов
 *
 * Все проверки оптимизированы для быстрого выполнения
 * и содержат детальные сообщения об ошибках.
 *
 * @author ScalpingBot Team
 * @version 1.0
 */
@Slf4j
public class ValidationUtils {

    /**
     * Регулярные выражения для валидации
     */
    private static final Pattern TRADING_PAIR_PATTERN = Pattern.compile("^[A-Z0-9]{2,10}(USDT|BTC|ETH|BNB|BUSD|USDC)$");
    private static final Pattern API_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9]{64}$");
    private static final Pattern SECRET_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9+/]{64,}={0,2}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    private static final Pattern TELEGRAM_CHAT_ID_PATTERN = Pattern.compile("^-?[0-9]{1,15}$");

    /**
     * Константы для валидации
     */
    private static final BigDecimal MIN_PRICE = new BigDecimal("0.00000001");
    private static final BigDecimal MAX_PRICE = new BigDecimal("1000000");
    private static final BigDecimal MIN_QUANTITY = new BigDecimal("0.00000001");
    private static final BigDecimal MAX_QUANTITY = new BigDecimal("1000000");
    private static final BigDecimal MIN_USDT_VALUE = new BigDecimal("10.0");
    private static final BigDecimal MAX_USDT_VALUE = new BigDecimal("1000000.0");

    // Приватный конструктор для утилитарного класса
    private ValidationUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Валидировать торговую пару
     *
     * @param tradingPair торговая пара
     * @return true если валидна
     */
    public static boolean isValidTradingPair(String tradingPair) {
        return tradingPair != null &&
                !tradingPair.trim().isEmpty() &&
                TRADING_PAIR_PATTERN.matcher(tradingPair.toUpperCase()).matches();
    }

    /**
     * Валидировать торговую пару с исключением
     *
     * @param tradingPair торговая пара
     * @throws IllegalArgumentException если не валидна
     */
    public static void validateTradingPair(String tradingPair) {
        if (!isValidTradingPair(tradingPair)) {
            throw new IllegalArgumentException(
                    String.format("Неверный формат торговой пары: %s. " +
                            "Ожидается формат: BTCUSDT, ETHUSDT, и т.д.", tradingPair)
            );
        }
    }

    /**
     * Валидировать цену
     *
     * @param price цена
     * @return true если валидна
     */
    public static boolean isValidPrice(BigDecimal price) {
        return price != null &&
                price.compareTo(MIN_PRICE) >= 0 &&
                price.compareTo(MAX_PRICE) <= 0;
    }

    /**
     * Валидировать цену с исключением
     *
     * @param price цена
     * @throws IllegalArgumentException если не валидна
     */
    public static void validatePrice(BigDecimal price) {
        if (!isValidPrice(price)) {
            throw new IllegalArgumentException(
                    String.format("Неверная цена: %s. Диапазон: %s - %s",
                            price, MIN_PRICE, MAX_PRICE)
            );
        }
    }

    /**
     * Валидировать количество
     *
     * @param quantity количество
     * @return true если валидно
     */
    public static boolean isValidQuantity(BigDecimal quantity) {
        return quantity != null &&
                quantity.compareTo(MIN_QUANTITY) >= 0 &&
                quantity.compareTo(MAX_QUANTITY) <= 0;
    }

    /**
     * Валидировать количество с исключением
     *
     * @param quantity количество
     * @throws IllegalArgumentException если не валидно
     */
    public static void validateQuantity(BigDecimal quantity) {
        if (!isValidQuantity(quantity)) {
            throw new IllegalArgumentException(
                    String.format("Неверное количество: %s. Диапазон: %s - %s",
                            quantity, MIN_QUANTITY, MAX_QUANTITY)
            );
        }
    }

    /**
     * Валидировать объем сделки в USDT
     *
     * @param volume объем в USDT
     * @return true если валиден
     */
    public static boolean isValidUsdtVolume(BigDecimal volume) {
        return volume != null &&
                volume.compareTo(MIN_USDT_VALUE) >= 0 &&
                volume.compareTo(MAX_USDT_VALUE) <= 0;
    }

    /**
     * Валидировать объем сделки с исключением
     *
     * @param volume объем в USDT
     * @throws IllegalArgumentException если не валиден
     */
    public static void validateUsdtVolume(BigDecimal volume) {
        if (!isValidUsdtVolume(volume)) {
            throw new IllegalArgumentException(
                    String.format("Неверный объем сделки: $%s. Диапазон: $%s - $%s",
                            volume, MIN_USDT_VALUE, MAX_USDT_VALUE)
            );
        }
    }

    /**
     * Валидировать процентное значение
     *
     * @param percentage процент
     * @param min минимальное значение
     * @param max максимальное значение
     * @return true если валиден
     */
    public static boolean isValidPercentage(BigDecimal percentage, BigDecimal min, BigDecimal max) {
        return percentage != null &&
                percentage.compareTo(min) >= 0 &&
                percentage.compareTo(max) <= 0;
    }

    /**
     * Валидировать процент прибыли для скальпинга
     *
     * @param profitPercent процент прибыли
     * @throws IllegalArgumentException если не валиден
     */
    public static void validateProfitPercent(BigDecimal profitPercent) {
        BigDecimal min = new BigDecimal("0.1");
        BigDecimal max = new BigDecimal("10.0");

        if (!isValidPercentage(profitPercent, min, max)) {
            throw new IllegalArgumentException(
                    String.format("Неверный процент прибыли: %s%%. Диапазон для скальпинга: %s%% - %s%%",
                            profitPercent, min, max)
            );
        }
    }

    /**
     * Валидировать процент стоп-лосса
     *
     * @param stopLossPercent процент стоп-лосса
     * @throws IllegalArgumentException если не валиден
     */
    public static void validateStopLossPercent(BigDecimal stopLossPercent) {
        BigDecimal min = new BigDecimal("0.1");
        BigDecimal max = new BigDecimal("5.0");

        if (!isValidPercentage(stopLossPercent, min, max)) {
            throw new IllegalArgumentException(
                    String.format("Неверный процент стоп-лосса: %s%%. Диапазон: %s%% - %s%%",
                            stopLossPercent, min, max)
            );
        }
    }

    /**
     * Валидировать соотношение риск/прибыль
     *
     * @param profitPercent процент прибыли
     * @param stopLossPercent процент стоп-лосса
     * @throws IllegalArgumentException если соотношение неприемлемо
     */
    public static void validateRiskRewardRatio(BigDecimal profitPercent, BigDecimal stopLossPercent) {
        if (profitPercent == null || stopLossPercent == null || stopLossPercent.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Проценты прибыли и стоп-лосса должны быть положительными");
        }

        BigDecimal ratio = profitPercent.divide(stopLossPercent, 2, BigDecimal.ROUND_HALF_UP);
        BigDecimal minRatio = new BigDecimal("1.5");
        BigDecimal maxRatio = new BigDecimal("5.0");

        if (ratio.compareTo(minRatio) < 0 || ratio.compareTo(maxRatio) > 0) {
            throw new IllegalArgumentException(
                    String.format("Неприемлемое соотношение риск/прибыль: 1:%s. Рекомендуется: 1:%s - 1:%s",
                            ratio, minRatio, maxRatio)
            );
        }
    }

    /**
     * Валидировать размер позиции относительно баланса
     *
     * @param positionSize размер позиции
     * @param accountBalance баланс счета
     * @param maxPositionPercent максимальный процент позиции
     * @throws IllegalArgumentException если размер превышает лимит
     */
    public static void validatePositionSize(BigDecimal positionSize, BigDecimal accountBalance,
                                            BigDecimal maxPositionPercent) {
        if (positionSize == null || accountBalance == null || maxPositionPercent == null) {
            throw new IllegalArgumentException("Все параметры должны быть указаны");
        }

        if (positionSize.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Размер позиции должен быть положительным");
        }

        BigDecimal maxPositionSize = accountBalance.multiply(maxPositionPercent).divide(new BigDecimal("100"));

        if (positionSize.compareTo(maxPositionSize) > 0) {
            throw new IllegalArgumentException(
                    String.format("Размер позиции $%s превышает лимит $%s (%s%% от баланса $%s)",
                            positionSize, maxPositionSize, maxPositionPercent, accountBalance)
            );
        }
    }

    /**
     * Валидировать количество открытых позиций
     *
     * @param currentPositions текущее количество позиций
     * @param maxPositions максимальное количество
     * @throws IllegalArgumentException если превышен лимит
     */
    public static void validatePositionCount(int currentPositions, int maxPositions) {
        if (currentPositions < 0 || maxPositions < 0) {
            throw new IllegalArgumentException("Количество позиций не может быть отрицательным");
        }

        if (currentPositions >= maxPositions) {
            throw new IllegalArgumentException(
                    String.format("Достигнут лимит позиций: %d из %d максимум",
                            currentPositions, maxPositions)
            );
        }
    }

    /**
     * Валидировать дневные потери
     *
     * @param currentLossPercent текущие потери в процентах
     * @param maxDailyLossPercent максимальный дневной лимит
     * @throws IllegalArgumentException если превышен лимит
     */
    public static void validateDailyLoss(BigDecimal currentLossPercent, BigDecimal maxDailyLossPercent) {
        if (currentLossPercent == null || maxDailyLossPercent == null) {
            throw new IllegalArgumentException("Параметры потерь должны быть указаны");
        }

        if (currentLossPercent.abs().compareTo(maxDailyLossPercent) >= 0) {
            throw new IllegalArgumentException(
                    String.format("Превышен дневной лимит потерь: %s%% из %s%% максимум",
                            currentLossPercent.abs(), maxDailyLossPercent)
            );
        }
    }

    /**
     * Валидировать API ключ
     *
     * @param apiKey API ключ
     * @return true если валиден
     */
    public static boolean isValidApiKey(String apiKey) {
        return apiKey != null &&
                !apiKey.trim().isEmpty() &&
                API_KEY_PATTERN.matcher(apiKey).matches();
    }

    /**
     * Валидировать секретный ключ
     *
     * @param secretKey секретный ключ
     * @return true если валиден
     */
    public static boolean isValidSecretKey(String secretKey) {
        return secretKey != null &&
                !secretKey.trim().isEmpty() &&
                SECRET_KEY_PATTERN.matcher(secretKey).matches();
    }

    /**
     * Валидировать API ключи с исключением
     *
     * @param apiKey API ключ
     * @param secretKey секретный ключ
     * @throws IllegalArgumentException если не валидны
     */
    public static void validateApiKeys(String apiKey, String secretKey) {
        if (!isValidApiKey(apiKey)) {
            throw new IllegalArgumentException("Неверный формат API ключа");
        }

        if (!isValidSecretKey(secretKey)) {
            throw new IllegalArgumentException("Неверный формат секретного ключа");
        }
    }

    /**
     * Валидировать email адрес
     *
     * @param email email адрес
     * @return true если валиден
     */
    public static boolean isValidEmail(String email) {
        return email != null &&
                !email.trim().isEmpty() &&
                EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Валидировать Telegram chat ID
     *
     * @param chatId chat ID
     * @return true если валиден
     */
    public static boolean isValidTelegramChatId(String chatId) {
        return chatId != null &&
                !chatId.trim().isEmpty() &&
                TELEGRAM_CHAT_ID_PATTERN.matcher(chatId).matches();
    }

    /**
     * Валидировать временной интервал
     *
     * @param intervalSeconds интервал в секундах
     * @param minSeconds минимальный интервал
     * @param maxSeconds максимальный интервал
     * @throws IllegalArgumentException если интервал не валиден
     */
    public static void validateTimeInterval(int intervalSeconds, int minSeconds, int maxSeconds) {
        if (intervalSeconds < minSeconds || intervalSeconds > maxSeconds) {
            throw new IllegalArgumentException(
                    String.format("Неверный временной интервал: %d сек. Диапазон: %d - %d сек",
                            intervalSeconds, minSeconds, maxSeconds)
            );
        }
    }

    /**
     * Валидировать время жизни позиции
     *
     * @param positionOpenTime время открытия позиции
     * @param maxPositionTimeMinutes максимальное время в минутах
     * @throws IllegalArgumentException если позиция слишком старая
     */
    public static void validatePositionAge(LocalDateTime positionOpenTime, int maxPositionTimeMinutes) {
        if (positionOpenTime == null) {
            throw new IllegalArgumentException("Время открытия позиции должно быть указано");
        }

        LocalDateTime now = LocalDateTime.now();
        long ageMinutes = java.time.Duration.between(positionOpenTime, now).toMinutes();

        if (ageMinutes >= maxPositionTimeMinutes) {
            throw new IllegalArgumentException(
                    String.format("Позиция превысила максимальное время удержания: %d мин из %d макс",
                            ageMinutes, maxPositionTimeMinutes)
            );
        }
    }

    /**
     * Валидировать спред
     *
     * @param bidPrice цена покупки
     * @param askPrice цена продажи
     * @param maxSpreadPercent максимальный спред в процентах
     * @throws IllegalArgumentException если спред слишком высокий
     */
    public static void validateSpread(BigDecimal bidPrice, BigDecimal askPrice, BigDecimal maxSpreadPercent) {
        if (bidPrice == null || askPrice == null || maxSpreadPercent == null) {
            throw new IllegalArgumentException("Все параметры спреда должны быть указаны");
        }

        if (askPrice.compareTo(bidPrice) <= 0) {
            throw new IllegalArgumentException("Цена продажи должна быть выше цены покупки");
        }

        BigDecimal spread = askPrice.subtract(bidPrice);
        BigDecimal spreadPercent = spread.divide(bidPrice, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100"));

        if (spreadPercent.compareTo(maxSpreadPercent) > 0) {
            throw new IllegalArgumentException(
                    String.format("Спред слишком высокий: %s%% > %s%% максимум",
                            spreadPercent, maxSpreadPercent)
            );
        }
    }

    /**
     * Валидировать волатильность
     *
     * @param currentVolatility текущая волатильность (ATR)
     * @param maxVolatility максимальная допустимая волатильность
     * @param riskLevel текущий уровень риска
     * @throws IllegalArgumentException если волатильность слишком высокая
     */
    public static void validateVolatility(BigDecimal currentVolatility, BigDecimal maxVolatility, RiskLevel riskLevel) {
        if (currentVolatility == null || maxVolatility == null) {
            throw new IllegalArgumentException("Параметры волатильности должны быть указаны");
        }

        if (currentVolatility.compareTo(maxVolatility) > 0) {
            throw new IllegalArgumentException(
                    String.format("Волатильность слишком высокая для уровня риска %s: %s%% > %s%% максимум",
                            riskLevel, currentVolatility, maxVolatility)
            );
        }
    }

    /**
     * Валидировать корреляцию между активами
     *
     * @param correlation коэффициент корреляции
     * @param maxCorrelation максимальная допустимая корреляция
     * @param pairs список торговых пар
     * @throws IllegalArgumentException если корреляция слишком высокая
     */
    public static void validateCorrelation(BigDecimal correlation, BigDecimal maxCorrelation, List<String> pairs) {
        if (correlation == null || maxCorrelation == null) {
            throw new IllegalArgumentException("Параметры корреляции должны быть указаны");
        }

        if (correlation.abs().compareTo(maxCorrelation) > 0) {
            throw new IllegalArgumentException(
                    String.format("Корреляция между активами слишком высокая: %s > %s максимум для пар: %s",
                            correlation.abs(), maxCorrelation,
                            pairs != null ? String.join(", ", pairs) : "неизвестно")
            );
        }
    }

    /**
     * Валидировать совместимость типа ордера и стороны
     *
     * @param orderType тип ордера
     * @param orderSide сторона ордера
     * @throws IllegalArgumentException если комбинация не валидна
     */
    public static void validateOrderTypeAndSide(OrderType orderType, OrderSide orderSide) {
        if (orderType == null || orderSide == null) {
            throw new IllegalArgumentException("Тип ордера и сторона должны быть указаны");
        }

        // Специальные проверки для OCO ордеров
        if (orderType == OrderType.OCO) {
            // OCO ордера содержат оба направления, поэтому сторона должна быть основной
            // Дополнительные проверки могут быть добавлены здесь
        }

        // Проверка совместимости для скальпинга
        if (!orderSide.isCompatibleForScalping(orderType)) {
            throw new IllegalArgumentException(
                    String.format("Комбинация %s + %s не подходит для скальпинг-стратегии",
                            orderType, orderSide)
            );
        }
    }

    /**
     * Валидировать параметры для стоп-лосса
     *
     * @param entryPrice цена входа
     * @param stopLossPrice цена стоп-лосса
     * @param orderSide сторона позиции
     * @throws IllegalArgumentException если параметры не валидны
     */
    public static void validateStopLossPrice(BigDecimal entryPrice, BigDecimal stopLossPrice, OrderSide orderSide) {
        if (entryPrice == null || stopLossPrice == null || orderSide == null) {
            throw new IllegalArgumentException("Все параметры стоп-лосса должны быть указаны");
        }

        if (orderSide == OrderSide.BUY) {
            // Для длинной позиции стоп-лосс должен быть ниже цены входа
            if (stopLossPrice.compareTo(entryPrice) >= 0) {
                throw new IllegalArgumentException(
                        String.format("Для длинной позиции стоп-лосс (%s) должен быть ниже цены входа (%s)",
                                stopLossPrice, entryPrice)
                );
            }
        } else {
            // Для короткой позиции стоп-лосс должен быть выше цены входа
            if (stopLossPrice.compareTo(entryPrice) <= 0) {
                throw new IllegalArgumentException(
                        String.format("Для короткой позиции стоп-лосс (%s) должен быть выше цены входа (%s)",
                                stopLossPrice, entryPrice)
                );
            }
        }
    }

    /**
     * Валидировать все параметры ордера
     *
     * @param tradingPair торговая пара
     * @param orderType тип ордера
     * @param orderSide сторона ордера
     * @param quantity количество
     * @param price цена (может быть null для рыночных ордеров)
     * @throws IllegalArgumentException если любой параметр не валиден
     */
    public static void validateOrderParameters(String tradingPair, OrderType orderType, OrderSide orderSide,
                                               BigDecimal quantity, BigDecimal price) {
        validateTradingPair(tradingPair);
        validateQuantity(quantity);
        validateOrderTypeAndSide(orderType, orderSide);

        // Проверяем цену только если она требуется для типа ордера
        if (orderType.isRequiresPrice() && price != null) {
            validatePrice(price);
        }

        // Валидируем объем сделки
        if (price != null) {
            BigDecimal volume = quantity.multiply(price);
            validateUsdtVolume(volume);
        }
    }

    /**
     * Проверить, является ли строка null или пустой
     *
     * @param value проверяемая строка
     * @return true если null или пустая
     */
    public static boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Проверить, является ли значение null или меньше/равно нулю
     *
     * @param value проверяемое значение
     * @return true если null или <= 0
     */
    public static boolean isNullOrZero(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) <= 0;
    }

    /**
     * Проверить, что все значения в списке положительные
     *
     * @param values список значений
     * @return true если все положительные
     */
    public static boolean areAllPositive(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return false;
        }

        return values.stream()
                .allMatch(value -> value != null && value.compareTo(BigDecimal.ZERO) > 0);
    }
}