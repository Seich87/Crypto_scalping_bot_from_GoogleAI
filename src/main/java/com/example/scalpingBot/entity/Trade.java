package com.example.scalpingBot.entity;

import com.example.scalpingBot.enums.OrderSide;
import com.example.scalpingBot.enums.OrderStatus;
import com.example.scalpingBot.enums.OrderType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA-сущность, представляющая одну совершенную сделку (trade).
 * Эта запись создается после успешного исполнения ордера на бирже.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Entity
@Table(name = "trades")
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exchange_trade_id", nullable = false, unique = true)
    private String exchangeTradeId;

    @Column(name = "trading_pair", nullable = false, length = 20)
    private String tradingPair;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false, length = 10)
    private OrderSide side;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private OrderType type;

    @Column(name = "price", nullable = false, precision = 19, scale = 8)
    private BigDecimal price;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity;

    @Column(name = "commission", precision = 19, scale = 8)
    private BigDecimal commission;

    @Column(name = "commission_asset", length = 10)
    private String commissionAsset;

    @Column(name = "execution_timestamp", nullable = false)
    private LocalDateTime executionTimestamp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}