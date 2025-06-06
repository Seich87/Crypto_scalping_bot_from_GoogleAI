package com.example.scalpingBot.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Entity
@Table(name = "strategy_configs")
public class StrategyConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trading_pair", nullable = false, unique = true)
    private String tradingPair;

    @Column(name = "strategy_name", nullable = false)
    private String strategyName;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    /**
     * Храним параметры в виде JSON в базе данных.
     * Это самый гибкий способ для хранения произвольных пар ключ-значение.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parameters", columnDefinition = "json")
    private Map<String, String> parameters;
}