package com.example.scalpingBot.dto;

import java.math.BigDecimal;

/**
 * DTO (запись) для хранения рассчитанных значений Полос Боллинджера.
 * @param upperBand Верхняя полоса.
 * @param middleBand Средняя полоса (SMA).
 * @param lowerBand Нижняя полоса.
 */
public record BollingerBands(
        BigDecimal upperBand,
        BigDecimal middleBand,
        BigDecimal lowerBand
) {}