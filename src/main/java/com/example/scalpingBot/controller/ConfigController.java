package com.example.scalpingBot.controller;

import com.example.scalpingBot.dto.request.StrategyConfigRequest;
import com.example.scalpingBot.service.config.TradingConfigService;
import com.example.scalpingBot.service.trading.StrategyManager;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final TradingConfigService tradingConfigService;
    private final StrategyManager strategyManager;

    @Autowired
    public ConfigController(TradingConfigService tradingConfigService, StrategyManager strategyManager) {
        this.tradingConfigService = tradingConfigService;
        this.strategyManager = strategyManager;
    }

    /**
     * Получает список всех доступных имен стратегий в системе.
     * @return {"strategies": ["BOLLINGER_BANDS", "SMA_CROSSOVER"]}
     */
    @GetMapping("/strategies")
    public ResponseEntity<Map<String, List<String>>> getAvailableStrategies() {
        return ResponseEntity.ok(Map.of("strategies", strategyManager.getAvailableStrategyNames()));
    }

    /**
     * Устанавливает или обновляет конфигурацию стратегии для торговой пары.
     * @param configRequest Тело запроса с конфигурацией.
     * @return Статус 201 Created.
     */
    @PostMapping("/strategies")
    public ResponseEntity<Void> setStrategyConfig(@Valid @RequestBody StrategyConfigRequest configRequest) {
        tradingConfigService.updateStrategyConfig(configRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Получает список всех текущих конфигураций стратегий.
     * @return Список конфигураций.
     */
    @GetMapping("/strategies/active")
    public ResponseEntity<List<StrategyConfigRequest>> getAllStrategyConfigs() {
        return ResponseEntity.ok(tradingConfigService.getAllStrategyConfigs());
    }

    /**
     * Удаляет конфигурацию стратегии для указанной торговой пары,
     * фактически останавливая торговлю по ней.
     * @param symbol Торговая пара.
     * @return Статус 204 No Content.
     */
    @DeleteMapping("/strategies")
    public ResponseEntity<Void> removeStrategyConfig(@RequestParam("pair") String symbol) {
        tradingConfigService.removeStrategyConfig(symbol);
        return ResponseEntity.noContent().build();
    }
}