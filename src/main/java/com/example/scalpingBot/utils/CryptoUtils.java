package com.example.scalpingBot.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.TreeMap;

/**
 * Криптографические утилиты для скальпинг-бота
 *
 * Основные функции:
 * - Подпись запросов к API бирж (HMAC-SHA256)
 * - Генерация безопасных подписей для Binance, Bybit
 * - Хеширование паролей и чувствительных данных
 * - Генерация случайных значений для nonce
 * - Создание и проверка API подписей
 *
 * Все операции выполняются с использованием стандартных
 * криптографических алгоритмов Java для обеспечения
 * совместимости с требованиями бирж.
 *
 * @author ScalpingBot Team
 * @version 1.0
 */
@Slf4j
public class CryptoUtils {

    /**
     * Алгоритмы хеширования и подписи
     */
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SHA256 = "SHA-256";
    private static final String MD5 = "MD5";

    /**
     * Генератор случайных чисел
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // Приватный конструктор для утилитарного класса
    private CryptoUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Создать HMAC-SHA256 подпись для API запросов
     *
     * @param data данные для подписи
     * @param secretKey секретный ключ
     * @return подпись в hex формате
     * @throws RuntimeException если ошибка создания подписи
     */
    public static String createHmacSha256Signature(String data, String secretKey) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256
            );
            mac.init(secretKeySpec);

            byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(signature);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to create HMAC-SHA256 signature", e);
            throw new RuntimeException("Error creating signature: " + e.getMessage(), e);
        }
    }

    /**
     * Создать подпись для запросов к Binance API
     *
     * @param queryString строка запроса
     * @param secretKey секретный ключ Binance
     * @return подпись в hex формате
     */
    public static String createBinanceSignature(String queryString, String secretKey) {
        log.debug("Creating Binance signature for query: {}", queryString);
        return createHmacSha256Signature(queryString, secretKey);
    }

    /**
     * Создать подпись для запросов к Bybit API
     *
     * @param timestamp временная метка
     * @param apiKey API ключ
     * @param params параметры запроса
     * @param secretKey секретный ключ Bybit
     * @return подпись в hex формате
     */
    public static String createBybitSignature(long timestamp, String apiKey,
                                              Map<String, Object> params, String secretKey) {
        // Создаем отсортированную строку параметров
        StringBuilder paramString = new StringBuilder();
        paramString.append("api_key=").append(apiKey);
        paramString.append("&timestamp=").append(timestamp);

        if (params != null && !params.isEmpty()) {
            TreeMap<String, Object> sortedParams = new TreeMap<>(params);
            for (Map.Entry<String, Object> entry : sortedParams.entrySet()) {
                paramString.append("&").append(entry.getKey()).append("=").append(entry.getValue());
            }
        }

        String dataToSign = paramString.toString();
        log.debug("Creating Bybit signature for data: {}", dataToSign);

        return createHmacSha256Signature(dataToSign, secretKey);
    }

    /**
     * Создать подпись для WebSocket аутентификации Bybit
     *
     * @param apiKey API ключ
     * @param expires время истечения
     * @param secretKey секретный ключ
     * @return подпись в hex формате
     */
    public static String createBybitWebSocketSignature(String apiKey, long expires, String secretKey) {
        String data = "GET/realtime" + expires;
        return createHmacSha256Signature(data, secretKey);
    }

    /**
     * Создать SHA256 хеш строки
     *
     * @param input входная строка
     * @return хеш в hex формате
     */
    public static String sha256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA256);
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(hash);

        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to create SHA256 hash", e);
            throw new RuntimeException("Error creating hash: " + e.getMessage(), e);
        }
    }

    /**
     * Создать MD5 хеш строки
     *
     * @param input входная строка
     * @return хеш в hex формате
     */
    public static String md5Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(MD5);
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(hash);

        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to create MD5 hash", e);
            throw new RuntimeException("Error creating hash: " + e.getMessage(), e);
        }
    }

    /**
     * Хешировать пароль с солью
     *
     * @param password пароль
     * @param salt соль
     * @return хешированный пароль
     */
    public static String hashPassword(String password, String salt) {
        String combined = password + salt;
        return sha256Hash(combined);
    }

    /**
     * Генерировать случайную соль
     *
     * @param length длина соли в байтах
     * @return соль в hex формате
     */
    public static String generateSalt(int length) {
        byte[] salt = new byte[length];
        SECURE_RANDOM.nextBytes(salt);
        return Hex.encodeHexString(salt);
    }

    /**
     * Генерировать случайную соль (32 байта по умолчанию)
     *
     * @return соль в hex формате
     */
    public static String generateSalt() {
        return generateSalt(32);
    }

    /**
     * Генерировать nonce для API запросов
     *
     * @return случайное число
     */
    public static long generateNonce() {
        return SECURE_RANDOM.nextLong();
    }

    /**
     * Генерировать случайную строку заданной длины
     *
     * @param length длина строки
     * @return случайная строка
     */
    public static String generateRandomString(int length) {
        byte[] randomBytes = new byte[length / 2 + 1];
        SECURE_RANDOM.nextBytes(randomBytes);
        String randomString = Hex.encodeHexString(randomBytes);
        return randomString.substring(0, length);
    }

    /**
     * Кодировать строку в Base64
     *
     * @param input входная строка
     * @return строка в Base64
     */
    public static String encodeBase64(String input) {
        return Base64.encodeBase64String(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Декодировать строку из Base64
     *
     * @param input строка в Base64
     * @return декодированная строка
     */
    public static String decodeBase64(String input) {
        byte[] decoded = Base64.decodeBase64(input);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    /**
     * Создать строку запроса из параметров для подписи
     *
     * @param params параметры запроса
     * @return строка запроса
     */
    public static String createQueryString(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }

        StringBuilder queryString = new StringBuilder();

        // Сортируем параметры для консистентности
        TreeMap<String, Object> sortedParams = new TreeMap<>(params);

        boolean first = true;
        for (Map.Entry<String, Object> entry : sortedParams.entrySet()) {
            if (!first) {
                queryString.append("&");
            }
            queryString.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }

        return queryString.toString();
    }

    /**
     * Создать подпись для конкретного метода API Binance
     *
     * @param method HTTP метод (GET, POST, DELETE)
     * @param endpoint путь API
     * @param params параметры запроса
     * @param timestamp временная метка
     * @param secretKey секретный ключ
     * @return подпись
     */
    public static String createBinanceOrderSignature(String method, String endpoint,
                                                     Map<String, Object> params, long timestamp,
                                                     String secretKey) {
        StringBuilder dataToSign = new StringBuilder();

        // Добавляем timestamp
        params.put("timestamp", timestamp);

        // Создаем query string
        String queryString = createQueryString(params);
        dataToSign.append(queryString);

        log.debug("Creating Binance {} signature for endpoint: {} with data: {}",
                method, endpoint, queryString);

        return createHmacSha256Signature(dataToSign.toString(), secretKey);
    }

    /**
     * Проверить валидность подписи
     *
     * @param data данные для проверки
     * @param signature проверяемая подпись
     * @param secretKey секретный ключ
     * @return true если подпись валидна
     */
    public static boolean verifySignature(String data, String signature, String secretKey) {
        try {
            String expectedSignature = createHmacSha256Signature(data, secretKey);
            return expectedSignature.equals(signature);
        } catch (Exception e) {
            log.error("Error verifying signature", e);
            return false;
        }
    }

    /**
     * Создать безопасный API ключ
     *
     * @return новый API ключ
     */
    public static String generateApiKey() {
        return generateRandomString(64);
    }

    /**
     * Создать безопасный секретный ключ
     *
     * @return новый секретный ключ в Base64
     */
    public static String generateSecretKey() {
        byte[] secretBytes = new byte[64];
        SECURE_RANDOM.nextBytes(secretBytes);
        return Base64.encodeBase64String(secretBytes);
    }

    /**
     * Маскировать API ключ для логирования
     *
     * @param apiKey API ключ
     * @return замаскированный ключ
     */
    public static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            return "***";
        }

        String prefix = apiKey.substring(0, 4);
        String suffix = apiKey.substring(apiKey.length() - 4);
        return prefix + "****" + suffix;
    }

    /**
     * Маскировать секретный ключ для логирования
     *
     * @param secretKey секретный ключ
     * @return замаскированный ключ
     */
    public static String maskSecretKey(String secretKey) {
        if (secretKey == null || secretKey.length() < 8) {
            return "***";
        }

        return secretKey.substring(0, 4) + "****";
    }

    /**
     * Создать подпись для WebSocket соединения с Binance
     *
     * @param listenKey ключ для WebSocket
     * @param secretKey секретный ключ
     * @return подпись для WebSocket
     */
    public static String createBinanceWebSocketSignature(String listenKey, String secretKey) {
        return createHmacSha256Signature(listenKey, secretKey);
    }

    /**
     * Валидировать формат API ключа
     *
     * @param apiKey API ключ
     * @return true если формат корректен
     */
    public static boolean isValidApiKeyFormat(String apiKey) {
        return apiKey != null &&
                apiKey.length() == 64 &&
                apiKey.matches("^[a-zA-Z0-9]+$");
    }

    /**
     * Валидировать формат секретного ключа
     *
     * @param secretKey секретный ключ
     * @return true если формат корректен
     */
    public static boolean isValidSecretKeyFormat(String secretKey) {
        return secretKey != null &&
                secretKey.length() >= 64 &&
                Base64.isBase64(secretKey);
    }

    /**
     * Создать checksum для данных
     *
     * @param data данные
     * @return checksum в hex формате
     */
    public static String createChecksum(String data) {
        return sha256Hash(data).substring(0, 8); // Первые 8 символов хеша
    }

    /**
     * Проверить checksum данных
     *
     * @param data данные
     * @param expectedChecksum ожидаемый checksum
     * @return true если checksum совпадает
     */
    public static boolean verifyChecksum(String data, String expectedChecksum) {
        String actualChecksum = createChecksum(data);
        return actualChecksum.equals(expectedChecksum);
    }

    /**
     * Создать подпись для внутренней аутентификации
     *
     * @param userId ID пользователя
     * @param timestamp временная метка
     * @param secretKey внутренний секретный ключ
     * @return подпись для внутренней аутентификации
     */
    public static String createInternalSignature(String userId, long timestamp, String secretKey) {
        String data = userId + ":" + timestamp;
        return createHmacSha256Signature(data, secretKey);
    }

    /**
     * Создать токен сессии
     *
     * @param userId ID пользователя
     * @param sessionId ID сессии
     * @param secretKey секретный ключ для токенов
     * @return токен сессии
     */
    public static String createSessionToken(String userId, String sessionId, String secretKey) {
        long timestamp = System.currentTimeMillis();
        String data = userId + ":" + sessionId + ":" + timestamp;
        String signature = createHmacSha256Signature(data, secretKey);

        // Объединяем данные и подпись
        String tokenData = data + ":" + signature;
        return encodeBase64(tokenData);
    }

    /**
     * Проверить токен сессии
     *
     * @param token токен для проверки
     * @param secretKey секретный ключ для токенов
     * @param maxAgeMillis максимальный возраст токена в миллисекундах
     * @return true если токен валиден
     */
    public static boolean verifySessionToken(String token, String secretKey, long maxAgeMillis) {
        try {
            String decodedToken = decodeBase64(token);
            String[] parts = decodedToken.split(":");

            if (parts.length != 4) {
                return false;
            }

            String userId = parts[0];
            String sessionId = parts[1];
            long timestamp = Long.parseLong(parts[2]);
            String signature = parts[3];

            // Проверяем возраст токена
            long currentTime = System.currentTimeMillis();
            if (currentTime - timestamp > maxAgeMillis) {
                return false;
            }

            // Проверяем подпись
            String data = userId + ":" + sessionId + ":" + timestamp;
            String expectedSignature = createHmacSha256Signature(data, secretKey);

            return expectedSignature.equals(signature);

        } catch (Exception e) {
            log.error("Error verifying session token", e);
            return false;
        }
    }

    /**
     * Генерировать уникальный ID запроса
     *
     * @return уникальный ID
     */
    public static String generateRequestId() {
        long timestamp = System.currentTimeMillis();
        String randomPart = generateRandomString(8);
        return timestamp + "_" + randomPart;
    }

    /**
     * Создать fingerprint для конфигурации бота
     *
     * @param config строка конфигурации
     * @return fingerprint конфигурации
     */
    public static String createConfigFingerprint(String config) {
        return sha256Hash(config).substring(0, 16);
    }
}