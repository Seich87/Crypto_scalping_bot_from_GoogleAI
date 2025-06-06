package com.example.scalpingBot.dto.response;

import com.example.scalpingBot.enums.OrderSide;
import com.example.scalpingBot.enums.OrderStatus;
import com.example.scalpingBot.enums.OrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO для представления информации о совершенной сделке в ответах API.
 * Этот объект будет сериализован в JSON и отправлен клиенту.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeResponse {

    /**
     * Уникальный идентификатор сделки, присвоенный биржей.
     */
    private String exchangeTradeId;

    /**
     * Торговая пара.
     */
    private String tradingPair;

    /**
     * Статус сделки (например, FILLED).
     */
    private OrderStatus status;

    /**
     * Сторона сделки (BUY или SELL).
     */
    private OrderSide side;

    /**
     * Тип ордера (MARKET или LIMIT).
     */
    private OrderType type;

    /**
     * Цена исполнения сделки.
     */
    private BigDecimal price;

    /**
     * Количество исполненного актива.
     */
    private BigDecimal quantity;

    /**
     * Уплаченная комиссия.
     */
    private BigDecimal commission;

    /**
     * Актив, в котором была уплачена комиссия (например, USDT или BNB).
     */
    private String commissionAsset;

    /**
     * Временная метка исполнения сделки на бирже.
     */
    private LocalDateTime executionTimestamp;
}