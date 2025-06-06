package com.example.scalpingBot.dto.request;

import com.example.scalpingBot.enums.RiskLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO для запроса на обновление параметров управления рисками для конкретной торговой пары.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskParametersRequest {

    @NotBlank(message = "Trading pair symbol cannot be blank")
    private String tradingPair;

    /**
     * Процент от цены входа для установки стоп-лосса.
     * Например, значение 1.5 означает стоп-лосс на уровне -1.5% от цены входа.
     */
    @Positive(message = "Stop loss percentage must be a positive number")
    private BigDecimal stopLossPercentage;

    /**
     * Процент от цены входа для установки тейк-профита.
     * Например, значение 3.0 означает тейк-профит на уровне +3.0% от цены входа.
     */
    @Positive(message = "Take profit percentage must be a positive number")
    private BigDecimal takeProfitPercentage;

    /**
     * Максимальный размер одной позиции в quote-валюте (например, в USDT).
     */
    @Positive(message = "Maximum position size must be a positive number")
    private BigDecimal maxPositionSize;

    /**
     * Предустановленный уровень риска. Может использоваться для автоматической
     * установки всех остальных параметров (stopLoss, takeProfit и т.д.).
     */
    private RiskLevel riskLevel;
}