package com.example.scalpingBot.service.trading;

import com.example.scalpingBot.exception.TradingException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StrategyManager {

    private final Map<String, TradingStrategy> strategies;

    // Spring автоматически внедрит сюда список всех бинов, реализующих TradingStrategy
    public StrategyManager(List<TradingStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(TradingStrategy::getName, Function.identity()));
    }

    public TradingStrategy getStrategy(String name) {
        TradingStrategy strategy = strategies.get(name);
        if (strategy == null) {
            throw new TradingException("Strategy not found: " + name);
        }
        return strategy;
    }

    public List<String> getAvailableStrategyNames() {
        return strategies.keySet().stream().sorted().collect(Collectors.toList());
    }
}