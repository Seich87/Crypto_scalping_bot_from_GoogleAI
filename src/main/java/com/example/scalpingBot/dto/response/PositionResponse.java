package com.example.scalpingBot.dto.response;

import com.example.scalpingBot.enums.OrderSide;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO для представления информации о торговой позиции в ответах API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionResponse {

    /**
     * Торговая пара.
     */
    private String tradingPair;

    /**
     * Сторона позиции (BUY для long, SELL для short).
     */
    private OrderSide side;

    /**
     * Количество актива в позиции.
     */
    private BigDecimal quantity;

    /**
     * Средняя цена входа в позицию.
     */
    private BigDecimal entryPrice;

    /**
     * Текущая рыночная цена актива.
     * Это поле заполняется в реальном времени для расчета нереализованной прибыли/убытка.
     */
    private BigDecimal currentPrice;

    /**
     * Установленная цена стоп-лосса.
     */
    private BigDecimal stopLossPrice;

    /**
     * Установленная цена тейк-профита.
     */
    private BigDecimal takeProfitPrice;

    /**
     * Статус позиции (true - активна, false - закрыта).
     */
    private boolean isActive;

    /**
     * Нереализованная прибыль или убыток для активных позиций.
     * Рассчитывается как (currentPrice - entryPrice) * quantity.
     */
    private BigDecimal unrealizedPnl;

    /**
     * Реализованная (зафиксированная) прибыль или убыток для закрытых позиций.
     */
    private BigDecimal realizedPnl;

    /**
     * Временная метка открытия позиции.
     */
    private LocalDateTime openTimestamp;

    /**
     * Временная метка закрытия позиции (null для активных позиций).
     */
    private LocalDateTime closeTimestamp;
}