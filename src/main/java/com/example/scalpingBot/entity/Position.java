package com.example.scalpingBot.entity;

import com.example.scalpingBot.enums.OrderSide;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA-сущность, представляющая торговую позицию.
 * Позиция - это совокупность сделок по одному активу, открытая
 * с целью получения прибыли и впоследствии закрытая.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Entity
@Table(name = "positions", indexes = {
        @Index(name = "idx_positions_active_pair", columnList = "is_active, trading_pair")
})
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trading_pair", nullable = false, length = 20)
    private String tradingPair;

    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false, length = 10)
    private OrderSide side; // BUY (long) или SELL (short)

    @Column(name = "quantity", nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity;

    @Column(name = "entry_price", nullable = false, precision = 19, scale = 8)
    private BigDecimal entryPrice;

    @Column(name = "stop_loss_price", precision = 19, scale = 8)
    private BigDecimal stopLossPrice;

    @Column(name = "take_profit_price", precision = 19, scale = 8)
    private BigDecimal takeProfitPrice;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "open_timestamp", nullable = false, updatable = false)
    private LocalDateTime openTimestamp;

    @Column(name = "close_timestamp")
    private LocalDateTime closeTimestamp;

    @Column(name = "pnl", precision = 19, scale = 8)
    private BigDecimal pnl; // Profit and Loss

    @PrePersist
    protected void onCreate() {
        this.openTimestamp = LocalDateTime.now();
        this.isActive = true;
    }
}