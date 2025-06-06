package com.example.scalpingBot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA-сущность, представляющая событие, связанное с управлением рисками.
 * Используется для логирования и анализа срабатываний стоп-лоссов,
 * тейк-профитов и других риск-правил.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Entity
@Table(name = "risk_events", indexes = {
        @Index(name = "idx_riskevent_pair_time", columnList = "trading_pair, event_timestamp")
})
public class RiskEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Связь с позицией, к которой относится событие. Может быть null.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id")
    private Position position;

    @Column(name = "trading_pair", nullable = false, length = 20)
    private String tradingPair;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType; // Например, "STOP_LOSS_TRIGGERED", "LIQUIDATION_WARNING"

    @Column(name = "trigger_price", precision = 19, scale = 8)
    private BigDecimal triggerPrice;

    @Column(name = "message", length = 512)
    private String message;

    @Column(name = "event_timestamp", nullable = false, updatable = false)
    private LocalDateTime eventTimestamp;

    @PrePersist
    protected void onCreate() {
        eventTimestamp = LocalDateTime.now();
    }
}