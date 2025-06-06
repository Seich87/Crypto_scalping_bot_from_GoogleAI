package com.example.scalpingBot.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

/**
 * Класс-утилита для криптографических операций,
 * в основном для создания подписей для запросов к API бирж.
 */
public final class CryptoUtils {

    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    private CryptoUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Генерирует подпись HMAC-SHA256 для строки данных с использованием секретного ключа.
     * Этот метод используется для аутентификации защищенных эндпоинтов API биржи.
     *
     * @param data   Данные для подписи (например, строка запроса "symbol=BTCUSDT&side=BUY×tamp=...").
     * @param secret Секретный ключ API, который должен храниться в безопасности.
     * @return Подпись в виде шестнадцатеричной строки в нижнем регистре.
     */
    public static String generateHmacSha256(String data, String secret) {
        try {
            // Получаем экземпляр Mac для алгоритма HmacSHA256
            Mac sha256_HMAC = Mac.getInstance(HMAC_SHA256_ALGORITHM);

            // Создаем спецификацию ключа из байтов секрета
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256_ALGORITHM);

            // Инициализируем Mac нашим ключом
            sha256_HMAC.init(secret_key);

            // Вычисляем HMAC и получаем байтовый массив
            byte[] hmacBytes = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // Конвертируем байтовый массив в шестнадцатеричную строку
            return toHexString(hmacBytes);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            // Эти исключения не должны возникать в стандартной среде Java,
            // так как HmacSHA256 является стандартным алгоритмом.
            // Если они все же возникли, это указывает на серьезную проблему с конфигурацией среды Java.
            throw new IllegalStateException("Failed to generate HMAC-SHA256 signature due to a system configuration error", e);
        }
    }

    /**
     * Вспомогательный метод для конвертации массива байтов в шестнадцатеричную строку.
     *
     * @param bytes Массив байтов.
     * @return Шестнадцатеричная строка.
     */
    private static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        // Используем Formatter для эффективного и потокобезопасного форматирования
        try (Formatter formatter = new Formatter(sb)) {
            for (byte b : bytes) {
                formatter.format("%02x", b);
            }
        }
        return sb.toString();
    }
}