package com.example.scalpingBot.utils;

import com.example.scalpingBot.enums.OrderSide;
import com.example.scalpingBot.enums.OrderType;
import com.example.scalpingBot.exception.TradingException;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Утилиты валидации для скальпинг-бота
 *
 * Основные функции:
 * - Валидация параметров торговых ордеров
 * - Проверка торговых пар и их форматов
 * - Валидация размеров позиций и цен
 * - Проверка лимитов и ограничений бирж
 * - Валидация временных параметров
 *
 * Все проверки соответствуют требованиям основных
 * криптовалютных бирж (Binance, Bybit).
 *
 * @author ScalpingBot Team
 * @version 1.0
 */
@Slf4j
public class ValidationUtils {

    /**
     * Регулярные выражения для валидации
     */
    private static final Pattern TRADING_PAIR_PATTERN = Pattern.compile("^[A-Z0-9]{2,10}(USDT|BTC|ETH|BNB|BUSD)$");
    private static final Pattern BINANCE_ORDER_ID_PATTERN = Pattern.compile("^[0-9]+$");
    private static final Pattern CLIENT_ORDER_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,36}$");

    /**
     * Минимальные и максимальные значения для торговли
     */
    private static final BigDecimal MIN_USDT_VOLUME = new BigDecimal("5.0");
    private static final BigDecimal MAX_USDT_VOLUME = new BigDecimal("1000000.0");
    private static final BigDecimal MIN_QUANTITY = new BigDecimal("0.00000001");
    private static final BigDecimal MAX_QUANTITY = new BigDecimal("1000000.0");
    private static final BigDecimal MIN_PRICE = new BigDecimal("0.00000001");
    private static final BigDecimal MAX_PRICE = new BigDecimal("1000000.0");

    /**
     * Максимальные размеры строк
     */
    private static final int MAX_TRADING_PAIR_LENGTH = 20;
    private static final int MAX_ORDER_ID_LENGTH = 50;

    // Приватный конструктор для утилитарного класса
    private ValidationUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Валидировать параметры торгового ордера
     *
     * @param tradingPair торговая пара
     * @param orderType тип ордера
     * @param orderSide сторона ордера
     * @param quantity количество
     * @param price цена (может быть null для рыночных ордеров)
     * @throws TradingException если параметры некорректны
     */
    public static void validateOrderParameters(String tradingPair, OrderType orderType,
                                               OrderSide orderSide, BigDecimal quantity, BigDecimal price) {
        log.debug("Validating order parameters: {} {} {} qty={} price={}",
                tradingPair, orderType, orderSide, quantity, price);

        // Валидация торговой пары
        validateTradingPair(tradingPair);

        // Валидация типа и стороны ордера
        validateNotNull(orderType, "Order type");
        validateNotNull(orderSide, "Order side");

        // Валидация количества
        validateQuantity(quantity);

        // Валидация цены (если требуется)
        if (orderType.isRequiresPrice() && price != null) {
            validatePrice(price);
        } else if (orderType == OrderType.MARKET && price != null) {
            log.warn("Price specified for market order, will be ignored");
        }

        // Специфичная валидация для типов ордеров
        validateOrderTypeSpecific(orderType, orderSide);

        log.debug("Order parameters validation passed");
    }

    /**
     * Валидировать торговую пару
     *
     * @param tradingPair торговая пара
     * @throws TradingException если пара некорректна
     */
    public static void validateTradingPair(String tradingPair) {
        validateNotBlank(tradingPair, "Trading pair");

        if (tradingPair.length() > MAX_TRADING_PAIR_LENGTH) {
            throw new TradingException(TradingException.TradingErrorType.INVALID_TRADING_PAIR,
                    "Trading pair too long: " + tradingPair.length() + " characters");
        }

        if (!TRADING_PAIR_PATTERN.matcher(tradingPair).matches()) {
            throw new TradingException(TradingException.TradingErrorType.INVALID_TRADING_PAIR,
                    "Invalid trading pair format: " + tradingPair);
        }

        log.debug("Trading pair validation passed: {}", tradingPair);
    }

    /**
     * Валидировать количество
     *
     * @param quantity количество
     * @throws TradingException если количество некорректно
     */
    public static void validateQuantity(BigDecimal quantity) {
        validateNotNull(quantity, "Quantity");

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TradingException(TradingException.TradingErrorType.INVALID_POSITION_SIZE,
                    "Quantity must be positive: " + quantity);
        }

        if (quantity.compareTo(MIN_QUANTITY) < 0) {
            throw new TradingException(TradingException.TradingErrorType.INVALID_POSITION_SIZE,
                    "Quantity below minimum: " + quantity + " < " + MIN_QUANTITY);
        }

        if (quantity.compareTo(MAX_QUANTITY) > 0) {
            throw new TradingException(TradingException.TradingErrorType.INVALID_POSITION_SIZE,
                    "Quantity above maximum: " + quantity + " > " + MAX_QUANTITY);
        }

        log.debug("Quantity validation passed: {}", quantity);
    }

    /**
     * Валидировать цену
     *
     * @param price цена
     * @throws TradingException если цена некорректна
     */
    public static void validatePrice(BigDecimal price) {
        validateNotNull(price, "Price");

        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TradingException(TradingException.TradingErrorType.INVALID_PRICE,
                    "Price must be positive: " + price);
        }

        if (price.compareTo(MIN_PRICE) < 0) {
            throw new TradingException(TradingException.TradingErrorType.INVALID_PRICE,
                    "Price below minimum: " + price + " < " + MIN_PRICE);
        }

        if (price.compareTo(MAX_PRICE) > 0) {
            throw new TradingException(TradingException.TradingErrorType.INVALID_PRICE,
                    "Price above maximum: " + price + " > " + MAX_PRICE);
        }

        log.debug("Price validation passed: {}", price);
    }

    /**
     * Валидировать объем в USDT
     *
     * @param volume объем в USDT
     * @throws TradingException если объем некорректен
     */
    public static void validateUsdtVolume(BigDecimal volume) {
        validateNotNull(volume, "USDT volume");

        if (volume.compareTo(MIN_USDT_VOLUME) < 0) {
            throw new TradingException(TradingException.TradingErrorType.INVALID_POSITION_SIZE,
                    "USDT volume below minimum: " + volume + " < " + MIN_USDT_VOLUME);
        }

        if (volume.compareTo(MAX_USDT_VOLUME) > 0) {
            throw new TradingException(TradingException.TradingErrorType.INVALID_POSITION_SIZE,
                    "USDT volume above maximum: " + volume + " > " + MAX_USDT_VOLUME);
        }

        log.debug("USDT volume validation passed: {}", volume);
    }

    /**
     * Валидировать ID ордера Binance
     *
     * @param orderId ID ордера
     * @throws TradingException если ID некорректен
     */
    public static void validateBinanceOrderId(String orderId) {
        validateNotBlank(orderId, "Binance order ID");

        if (orderId.length() > MAX_ORDER_ID_LENGTH) {
            throw new TradingException(TradingException.TradingErrorType.ORDER_FAILED,
                    "Order ID too long: " + orderId.length() + " characters");
        }

        if (!BINANCE_ORDER_ID_PATTERN.matcher(orderId).matches()) {
            throw new TradingException(TradingException.TradingErrorType.ORDER_FAILED,
                    "Invalid Binance order ID format: " + orderId);
        }

        log.debug("Binance order ID validation passed: {}", orderId);
    }

    /**
     * Валидировать клиентский ID ордера
     *
     * @param clientOrderId клиентский ID ордера
     * @throws TradingException если ID некорректен
     */
    public static void validateClientOrderId(String clientOrderId) {
        if (clientOrderId == null) {
            return; // Клиентский ID опционален
        }

        if (clientOrderId.trim().isEmpty()) {
            throw new TradingException(TradingException.TradingErrorType.ORDER_FAILED,
                    "Client order ID cannot be empty");
        }

        if (clientOrderId.length() > 36) {
            throw new TradingException(TradingException.TradingErrorType.ORDER_FAILED,
                    "Client order ID too long: " + clientOrderId.length() + " characters");
        }

        if (!CLIENT_ORDER_ID_PATTERN.matcher(clientOrderId).matches()) {
            throw new TradingException(TradingException.TradingErrorType.ORDER_FAILED,
                    "Invalid client order ID format: " + clientOrderId);
        }

        log.debug("Client order ID validation passed: {}", clientOrderId);
    }

    /**
     * Валидировать процентное значение
     *
     * @param percentage процент
     * @param fieldName название поля
     * @param min минимальное значение
     * @param max максимальное значение
     * @throws TradingException если процент некорректен
     */
    public static void validatePercentage(BigDecimal percentage, String fieldName,
                                          BigDecimal min, BigDecimal max) {
        validateNotNull(percentage, fieldName);

        if (percentage.compareTo(min) < 0) {
            throw new TradingException(TradingException.TradingErrorType.INVALID_POSITION_SIZE,
                    fieldName + " below minimum: " + percentage + "% < " + min + "%");
        }

        if (percentage.compareTo(max) > 0) {
            throw new TradingException(TradingException.TradingErrorType.INVALID_POSITION_SIZE,
                    fieldName + " above maximum: " + percentage + "% > " + max + "%");
        }

        log.debug("{} validation passed: {}%", fieldName, percentage);
    }

    /**
     * Валидировать размер позиции относительно баланса
     *
     * @param positionSize размер позиции
     * @param accountBalance баланс счета
     * @param maxPercentage максимальный процент от баланса
     * @throws TradingException если размер позиции некорректен
     */
    public static void validatePositionSizeVsBalance(BigDecimal positionSize, BigDecimal accountBalance,
                                                     BigDecimal maxPercentage) {
        validateNotNull(positionSize, "Position size");
        validateNotNull(accountBalance, "Account balance");
        validateNotNull(maxPercentage, "Max percentage");

        if (accountBalance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TradingException(TradingException.TradingErrorType.INSUFFICIENT_BALANCE,
                    "Account balance must be positive: " + accountBalance);
        }

        BigDecimal maxPositionSize = MathUtils.percentageOf(accountBalance, maxPercentage);

        if (positionSize.compareTo(maxPositionSize) > 0) {
            throw new TradingException(TradingException.TradingErrorType.POSITION_LIMIT_EXCEEDED,
                    String.format("Position size %s exceeds %s%% of balance (%s)",
                            positionSize, maxPercentage, maxPositionSize));
        }

        log.debug("Position size vs balance validation passed: {} / {} ({}%)",
                positionSize, accountBalance,
                MathUtils.percentageChange(BigDecimal.ZERO,
                        MathUtils.safeDivide(positionSize, accountBalance).multiply(new BigDecimal("100"))));
    }

    /**
     * Валидировать временную метку
     *
     * @param timestamp временная метка в миллисекундах
     * @param maxAgeSeconds максимальный возраст в секундах
     * @throws TradingException если временная метка некорректна
     */
    public static void validateTimestamp(long timestamp, int maxAgeSeconds) {
        long currentTime = System.currentTimeMillis();
        long age = (currentTime - timestamp) / 1000;

        if (age > maxAgeSeconds) {
            throw new TradingException(TradingException.TradingErrorType.ORDER_TIMEOUT,
                    "Timestamp too old: " + age + " seconds > " + maxAgeSeconds + " seconds");
        }

        if (timestamp > currentTime + 5000) { // 5 секунд в будущем
            throw new TradingException(TradingException.TradingErrorType.ORDER_FAILED,
                    "Timestamp in future: " + timestamp + " > " + currentTime);
        }

        log.debug("Timestamp validation passed: {} (age: {} seconds)", timestamp, age);
    }

    /**
     * Специфичная валидация для типов ордеров
     */
    private static void validateOrderTypeSpecific(OrderType orderType, OrderSide orderSide) {
        // Проверка совместимости типа ордера со стороной
        if (!orderSide.isCompatibleForScalping(orderType)) {
            throw new TradingException(TradingException.TradingErrorType.INVALID_POSITION_SIZE,
                    "Order type " + orderType + " not compatible with side " + orderSide + " for scalping");
        }

        log.debug("Order type specific validation passed: {} {}", orderType, orderSide);
    }

    /**
     * Валидировать что объект не null
     */
    private static void validateNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new TradingException(TradingException.TradingErrorType.INVALID_POSITION_SIZE,
                    fieldName + " cannot be null");
        }
    }

    /**
     * Валидировать что строка не пустая
     */
    private static void validateNotBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new TradingException(TradingException.TradingErrorType.INVALID_TRADING_PAIR,
                    fieldName + " cannot be blank");
        }
    }

    /**
     * Валидировать API ключ
     *
     * @param apiKey API ключ
     * @throws TradingException если ключ некорректен
     */
    public static void validateApiKey(String apiKey) {
        validateNotBlank(apiKey, "API key");

        if (apiKey.length() < 32) {
            throw new TradingException(TradingException.TradingErrorType.API_ERROR,
                    "API key too short");
        }

        if (apiKey.length() > 128) {
            throw new TradingException(TradingException.TradingErrorType.API_ERROR,
                    "API key too long");
        }

        log.debug("API key validation passed: {}", CryptoUtils.maskApiKey(apiKey));
    }

    /**
     * Валидировать секретный ключ
     *
     * @param secretKey секретный ключ
     * @throws TradingException если ключ некорректен
     */
    public static void validateSecretKey(String secretKey) {
        validateNotBlank(secretKey, "Secret key");

        if (secretKey.length() < 32) {
            throw new TradingException(TradingException.TradingErrorType.API_ERROR,
                    "Secret key too short");
        }

        if (secretKey.length() > 128) {
            throw new TradingException(TradingException.TradingErrorType.API_ERROR,
                    "Secret key too long");
        }

        log.debug("Secret key validation passed: {}", CryptoUtils.maskSecretKey(secretKey));
    }

    /**
     * Проверить диапазон значений
     *
     * @param value проверяемое значение
     * @param min минимум
     * @param max максимум
     * @param fieldName название поля
     * @return true если значение в диапазоне
     */
    public static boolean isInRange(BigDecimal value, BigDecimal min, BigDecimal max, String fieldName) {
        if (value == null) {
            log.warn("{} is null, cannot check range", fieldName);
            return false;
        }

        boolean inRange = value.compareTo(min) >= 0 && value.compareTo(max) <= 0;

        if (!inRange) {
            log.warn("{} {} is outside range [{}, {}]", fieldName, value, min, max);
        }

        return inRange;
    }
}