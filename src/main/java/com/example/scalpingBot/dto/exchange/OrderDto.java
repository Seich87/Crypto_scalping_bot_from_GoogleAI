package com.example.scalpingBot.dto.exchange;

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
 * Стандартизированный DTO для представления информации об ордере, полученной от биржи.
 * Этот класс служит внутренним, унифицированным форматом, в который преобразуются
 * данные из ответов API различных бирж (Binance, Bybit и т.д.).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {

    /**
     * Уникальный ID ордера, присвоенный биржей.
     */
    private String exchangeOrderId;

    /**
     * ID ордера, который мы могли отправить при его создании (clientOrderId).
     * Используется для сопоставления.
     */
    private String clientOrderId;

    /**
     * Торговая пара (символ), например "BTCUSDT".
     */
    private String symbol;

    /**
     * Сторона ордера (BUY/SELL).
     */
    private OrderSide side;

    /**
     * Тип ордера (MARKET/LIMIT).
     */
    private OrderType type;

    /**
     * Текущий статус ордера на бирже (NEW, FILLED, CANCELED и т.д.).
     */
    private OrderStatus status;

    /**
     * Запрошенная цена (для лимитных ордеров).
     */
    private BigDecimal price;

    /**
     * Изначально запрошенное количество в ордере.
     */
    private BigDecimal originalQuantity;

    /**
     * Фактически исполненное количество.
     */
    private BigDecimal executedQuantity;

    /**
     * Суммарная стоимость исполненной части ордера в quote-валюте (price * executedQuantity).
     */
    private BigDecimal cumulativeQuoteQuantity;

    /**
     * Временная метка создания ордера на бирже.
     */
    private LocalDateTime createdAt;

    /**
     * Временная метка последнего обновления ордера на бирже.
     */
    private LocalDateTime updatedAt;
}