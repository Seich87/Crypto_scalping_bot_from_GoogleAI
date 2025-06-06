package com.example.scalpingBot.utils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Класс-утилита для выполнения общих математических операций с высокой точностью,
 * используя BigDecimal.
 */
public final class MathUtils {

    /**
     * Стандартный контекст для деления, чтобы избежать ошибок при бесконечных дробях.
     * 32 знака точности - более чем достаточно для большинства финансовых расчетов.
     */
    private static final MathContext MC = new MathContext(32, RoundingMode.HALF_UP);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private MathUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Округляет BigDecimal до указанного количества знаков после запятой.
     * Критически важно для форматирования цен и количеств для отправки на биржу.
     *
     * @param value Значение для округления.
     * @param scale Количество знаков после запятой.
     * @return Округленное значение BigDecimal.
     */
    public static BigDecimal scale(BigDecimal value, int scale) {
        if (value == null) {
            return null;
        }
        return value.setScale(scale, RoundingMode.HALF_UP);
    }

    /**
     * Вычисляет процентное изменение между старым и новым значением.
     * Формула: ((newValue - oldValue) / |oldValue|) * 100.
     *
     * @param oldValue Начальное значение.
     * @param newValue Конечное значение.
     * @return Процентное изменение. Возвращает BigDecimal.ZERO, если oldValue равен нулю.
     */
    public static BigDecimal calculatePercentageChange(BigDecimal oldValue, BigDecimal newValue) {
        if (oldValue == null || newValue == null || oldValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal difference = newValue.subtract(oldValue);
        return difference.divide(oldValue.abs(), MC).multiply(HUNDRED);
    }

    /**
     * Применяет процент к базовому значению. Удобно для расчета уровней стоп-лосса и тейк-профита.
     * Пример для тейк-профита (+2.5%): applyPercentage(entryPrice, new BigDecimal("2.5"))
     * Пример для стоп-лосса (-1.5%):   applyPercentage(entryPrice, new BigDecimal("-1.5"))
     *
     * @param baseValue  Базовое значение (например, цена входа).
     * @param percentage Процент для применения (может быть положительным или отрицательным).
     * @return Новое значение после применения процента.
     */
    public static BigDecimal applyPercentage(BigDecimal baseValue, BigDecimal percentage) {
        if (baseValue == null || percentage == null) {
            return baseValue;
        }
        BigDecimal factor = BigDecimal.ONE.add(percentage.divide(HUNDRED, MC));
        return baseValue.multiply(factor);
    }

    /**
     * Проверяет, является ли значение BigDecimal положительным (строго больше нуля).
     *
     * @param value Значение для проверки.
     * @return true, если value > 0, иначе false.
     */
    public static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }
}