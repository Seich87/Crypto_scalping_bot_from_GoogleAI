package com.example.scalpingBot.utils;

import com.example.scalpingBot.exception.TradingException;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Класс-утилита для выполнения общих проверок (валидации) в бизнес-логике.
 * Методы этого класса обычно выбрасывают исключение, если проверка не пройдена.
 */
public final class ValidationUtils {

    private ValidationUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Проверяет, что объект не является null.
     *
     * @param object       Объект для проверки.
     * @param errorMessage Сообщение об ошибке, которое будет в исключении.
     * @throws TradingException если объект null.
     */
    public static void assertNotNull(Object object, String errorMessage) {
        if (Objects.isNull(object)) {
            throw new TradingException(errorMessage);
        }
    }

    /**
     * Проверяет, что строка не является пустой или null.
     *
     * @param str          Строка для проверки.
     * @param errorMessage Сообщение об ошибке, которое будет в исключении.
     * @throws TradingException если строка пустая или null.
     */
    public static void assertNotBlank(String str, String errorMessage) {
        if (str == null || str.trim().isEmpty()) {
            throw new TradingException(errorMessage);
        }
    }

    /**
     * Проверяет, что значение BigDecimal является положительным (строго больше нуля).
     *
     * @param value        Значение для проверки.
     * @param errorMessage Сообщение об ошибке, которое будет в исключении.
     * @throws TradingException если значение не является положительным.
     */
    public static void assertPositive(BigDecimal value, String errorMessage) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TradingException(errorMessage);
        }
    }

    /**
     * Проверяет истинность логического выражения, которое представляет собой бизнес-правило или состояние.
     *
     * @param expression   Логическое выражение для проверки.
     * @param errorMessage Сообщение об ошибке, которое будет в исключении, если выражение ложно.
     * @throws TradingException если выражение ложно (false).
     */
    public static void assertTrue(boolean expression, String errorMessage) {
        if (!expression) {
            throw new TradingException(errorMessage);
        }
    }
}