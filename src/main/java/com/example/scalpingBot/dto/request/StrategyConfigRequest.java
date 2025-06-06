package com.example.scalpingBot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO для запроса на настройку параметров торговой стратегии для конкретной пары.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StrategyConfigRequest {

    @NotBlank(message = "Trading pair symbol cannot be blank")
    private String tradingPair;

    @NotBlank(message = "Strategy name cannot be blank")
    private String strategyName; // Например, "SimpleMovingAverageStrategy"

    @NotNull(message = "isActive flag cannot be null")
    private Boolean isActive; // Включена ли стратегия для данной пары

    /**
     * Карта с параметрами, специфичными для данной стратегии.
     * Позволяет гибко настраивать любую стратегию, не меняя структуру DTO.
     * Пример:
     * {
     *   "short_window": "10",  // Короткое окно для скользящей средней
     *   "long_window": "50",   // Длинное окно для скользящей средней
     *   "rsi_period": "14",    // Период RSI
     *   "rsi_overbought": "70" // Уровень перекупленности RSI
     * }
     */
    private Map<String, String> parameters;
}