package com.example.scalpingBot.entity;

import com.example.scalpingBot.enums.TradingPairType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA-сущность, представляющая конфигурацию и метаданные торговой пары.
 * Хранит правила и ограничения для пары, такие как точность цены и количества.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Entity
@Table(name = "trading_pairs", indexes = {
        @Index(name = "idx_tpair_active_type", columnList = "is_active, type")
})
public class TradingPair {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol", nullable = false, unique = true, length = 20)
    private String symbol; // Например, "BTCUSDT"

    @Column(name = "base_asset", nullable = false, length = 10)
    private String baseAsset; // Например, "BTC"

    @Column(name = "quote_asset", nullable = false, length = 10)
    private String quoteAsset; // Например, "USDT"

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TradingPairType type;

    @Column(name = "is_active", nullable = false)
    private boolean isActive; // Активна ли пара для торговли ботом

    @Column(name = "price_precision", nullable = false)
    private int pricePrecision; // Количество знаков после запятой для цены

    @Column(name = "quantity_precision", nullable = false)
    private int quantityPrecision; // Количество знаков после запятой для количества

    @Column(name = "min_order_size", precision = 19, scale = 8)
    private BigDecimal minOrderSize; // Минимальный размер ордера в quote_asset

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}