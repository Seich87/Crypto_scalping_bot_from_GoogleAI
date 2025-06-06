package com.example.scalpingBot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * DTO для представления сводных метрик по управлению рисками и производительности бота.
 * Этот объект агрегирует данные по всем сделкам и позициям.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskMetricsResponse {

    /**
     * Общий реализованный профит/убыток (PnL) за все время.
     */
    private BigDecimal totalRealizedPnl;

    /**
     * Процент прибыльных сделок (Win Rate).
     * Рассчитывается как (количество прибыльных сделок / общее количество сделок) * 100.
     */
    private BigDecimal winRate;

    /**
     * Профит-фактор.
     * Рассчитывается как (общая сумма прибыли от прибыльных сделок / общая сумма убытков от убыточных сделок).
     * Значение > 1 указывает на прибыльную стратегию.
     */
    private BigDecimal profitFactor;

    /**
     * Максимальная просадка (Max Drawdown).
     * Максимальное падение баланса от пика до дна в процентах.
     */
    private BigDecimal maxDrawdown;

    /**
     * Общее количество совершенных сделок (закрытых позиций).
     */
    private int totalTrades;

    /**
     * Количество прибыльных сделок.
     */
    private int winningTrades;

    /**
     * Количество убыточных сделок.
     */
    private int losingTrades;

    /**
     * Средняя прибыль/убыток на одну сделку.
     */
    private BigDecimal averageTradePnl;

    /**
     * Средняя прибыль по прибыльным сделкам.
     */
    private BigDecimal averageWinningTrade;

    /**
     * Средний убыток по убыточным сделкам.
     */
    private BigDecimal averageLosingTrade;
}