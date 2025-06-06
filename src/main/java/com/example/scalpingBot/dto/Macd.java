package com.example.scalpingBot.dto;

import java.math.BigDecimal;

/**
 * DTO (запись) для хранения рассчитанных значений индикатора MACD.
 * @param macdLine Линия MACD (быстрая EMA - медленная EMA).
 * @param signalLine Сигнальная линия (EMA от линии MACD).
 * @param histogram Гистограмма (разница между линией MACD и сигнальной линией).
 */
public record Macd(
        BigDecimal macdLine,
        BigDecimal signalLine,
        BigDecimal histogram
) {}