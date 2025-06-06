package com.example.scalpingBot.dto.request;

import com.example.scalpingBot.enums.OrderSide;
import com.example.scalpingBot.enums.OrderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO для запроса на создание торгового ордера вручную через API.
 * Используется в контроллере для приема данных от пользователя.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeRequest {

    @NotBlank(message = "Trading pair symbol cannot be blank")
    private String tradingPair;

    @NotNull(message = "Order side cannot be null")
    private OrderSide side;

    @NotNull(message = "Order type cannot be null")
    private OrderType type;

    @NotNull(message = "Quantity cannot be null")
    @Positive(message = "Quantity must be a positive number")
    private BigDecimal quantity;

    /**
     * Цена, по которой должен быть исполнен ордер.
     * Обязательна для лимитных ордеров (LIMIT, STOP_LOSS_LIMIT и т.д.).
     * Для рыночных ордеров (MARKET) это поле может быть null.
     * Валидация этой бизнес-логики будет происходить в сервисном слое.
     */
    @Positive(message = "Price must be a positive number if provided")
    private BigDecimal price;
}