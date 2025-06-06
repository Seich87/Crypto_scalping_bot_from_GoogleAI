package com.example.scalpingBot.service.trading;

import com.example.scalpingBot.enums.OrderSide;
import java.util.Map;
import java.util.Optional;

/**
 * Интерфейс, определяющий контракт для всех торговых стратегий.
 */
public interface TradingStrategy {

    /**
     * Возвращает уникальное имя стратегии, которое будет использоваться для ее идентификации.
     * Например, "SMA_CROSSOVER" или "BOLLINGER_BANDS".
     *
     * @return имя стратегии.
     */
    String getName();

    /**
     * Генерирует торговый сигнал на основе анализа рыночных данных.
     *
     * @param symbol     Торговая пара.
     * @param parameters Карта с параметрами, специфичными для данной стратегии (например, периоды, множители).
     * @return Optional, содержащий OrderSide (BUY или SELL) если сигнал есть, или пустой Optional.
     */
    Optional<OrderSide> generateSignal(String symbol, Map<String, String> parameters);
}