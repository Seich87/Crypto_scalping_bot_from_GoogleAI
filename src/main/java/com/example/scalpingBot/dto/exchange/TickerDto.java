package com.example.scalpingBot.dto.exchange;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Стандартизированный DTO для представления рыночной информации (тикера) по торговой паре.
 * Служит внутренним унифицированным форматом для данных, получаемых из
 * WebSocket-стримов или REST API-запросов к различным биржам.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TickerDto {

    /**
     * Торговая пара (символ), например "BTCUSDT".
     */
    private String symbol;

    /**
     * Цена последней совершенной сделки.
     */
    private BigDecimal lastPrice;

    /**
     * Лучшая цена на покупку (самый высокий bid).
     */
    private BigDecimal bestBidPrice;

    /**
     * Лучшая цена на продажу (самый низкий ask).
     */
    private BigDecimal bestAskPrice;

    /**
     * Объем торгов за 24 часа в базовом активе (например, в BTC).
     */
    private BigDecimal volume24h;

    /**
     * Объем торгов за 24 часа в квотируемом активе (например, в USDT).
     */
    private BigDecimal quoteVolume24h;

    /**
     * Изменение цены за последние 24 часа.
     */
    private BigDecimal priceChangePercent24h;

    /**
     * Временная метка получения данных.
     */
    private LocalDateTime timestamp;
}