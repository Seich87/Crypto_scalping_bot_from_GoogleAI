package com.example.scalpingBot.dto.exchange;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * Стандартизированный DTO для представления информации о балансе одного актива на бирже.
 * Абстрагирует от специфичного формата ответа API конкретной биржи.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceDto {

    /**
     * Название актива (тикер), например, "BTC", "USDT".
     */
    private String asset;

    /**
     * Свободный баланс. Количество актива, доступное для создания новых ордеров.
     */
    private BigDecimal free;

    /**
     * Заблокированный баланс. Количество актива, зарезервированное в открытых ордерах.
     */
    private BigDecimal locked;
}