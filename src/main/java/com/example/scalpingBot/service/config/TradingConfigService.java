package com.example.scalpingBot.service.config;

import com.example.scalpingBot.dto.request.StrategyConfigRequest;
import com.example.scalpingBot.entity.StrategyConfig;
import com.example.scalpingBot.exception.TradingException;
import com.example.scalpingBot.repository.StrategyConfigRepository;
import com.example.scalpingBot.service.trading.StrategyManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TradingConfigService {

    private final StrategyManager strategyManager;
    private final StrategyConfigRepository configRepository;

    public TradingConfigService(StrategyManager strategyManager, StrategyConfigRepository configRepository) {
        this.strategyManager = strategyManager;
        this.configRepository = configRepository;
    }

    @Transactional
    public void updateStrategyConfig(StrategyConfigRequest request) {
        strategyManager.getStrategy(request.getStrategyName()); // Проверяем, что стратегия существует

        StrategyConfig config = configRepository.findByTradingPair(request.getTradingPair())
                .orElse(new StrategyConfig()); // Создаем новую, если не нашли

        config.setTradingPair(request.getTradingPair());
        config.setStrategyName(request.getStrategyName());
        config.setActive(request.getIsActive());
        config.setParameters(request.getParameters());

        configRepository.save(config);
    }

    @Transactional(readOnly = true)
    public StrategyConfigRequest getStrategyConfig(String tradingPair) {
        return configRepository.findByTradingPair(tradingPair)
                .map(this::mapEntityToDto)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<StrategyConfigRequest> getAllStrategyConfigs() {
        return configRepository.findAll().stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeStrategyConfig(String tradingPair) {
        StrategyConfig config = configRepository.findByTradingPair(tradingPair)
                .orElseThrow(() -> new TradingException("No strategy configuration found for pair: " + tradingPair));
        configRepository.delete(config);
    }

    private StrategyConfigRequest mapEntityToDto(StrategyConfig entity) {
        return new StrategyConfigRequest(
                entity.getTradingPair(),
                entity.getStrategyName(),
                entity.isActive(),
                entity.getParameters()
        );
    }
}