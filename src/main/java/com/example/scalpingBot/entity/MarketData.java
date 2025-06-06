package com.example.scalpingBot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA-сущность для хранения снимков рыночных данных (тиков) в определенный момент времени.
 * Эти данные собираются периодически и используются для анализа рынка.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Entity
@Table(name = "market_data", indexes = {
        @Index(name = "idx_marketdata_pair_time", columnList = "trading_pair, timestamp")
})
public class MarketData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trading_pair", nullable = false, length = 20)
    private String tradingPair;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "last_price", nullable = false, precision = 19, scale = 8)
    private BigDecimal lastPrice;

    @Column(name = "best_bid_price", precision = 19, scale = 8)
    private BigDecimal bestBidPrice; // Лучшая цена покупки

    @Column(name = "best_ask_price", precision = 19, scale = 8)
    private BigDecimal bestAskPrice; // Лучшая цена продажи

    @Column(name = "volume_24h", precision = 24, scale = 8)
    private BigDecimal volume24h; // Объем торгов за 24 часа

}