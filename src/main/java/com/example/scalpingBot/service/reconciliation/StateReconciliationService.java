package com.example.scalpingBot.service.reconciliation;

import com.example.scalpingBot.dto.request.StrategyConfigRequest;
import com.example.scalpingBot.entity.Position;
import com.example.scalpingBot.enums.OrderSide;
import com.example.scalpingBot.repository.PositionRepository;
import com.example.scalpingBot.service.exchange.ExchangeApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.scalpingBot.service.config.TradingConfigService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class StateReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(StateReconciliationService.class);
    private final TradingConfigService tradingConfigService;

    private final ExchangeApiService exchangeApiService;
    private final PositionRepository positionRepository;
    private final List<String> tradingPairs;

    @Autowired
    public StateReconciliationService(TradingConfigService tradingConfigService, @Qualifier("binance") ExchangeApiService exchangeApiService,
                                      PositionRepository positionRepository,
                                      @Value("${bot.trading-pairs}") List<String> tradingPairs) {
        this.tradingConfigService = tradingConfigService;
        this.exchangeApiService = exchangeApiService;
        this.positionRepository = positionRepository;
        this.tradingPairs = tradingPairs;
    }

    /**
     * Этот метод запускается один раз после того, как приложение полностью готово.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void reconcileStateOnStartup() {
        log.info("===== Starting State Reconciliation on Startup =====");

        for (String pair : tradingPairs) {
            try {
                reconcilePositionsForPair(pair);
                // Сверку открытых ордеров можно добавить по аналогии
                // reconcileOpenOrdersForPair(pair);
            } catch (Exception e) {
                log.error("Failed to reconcile state for pair {}: {}", pair, e.getMessage());
                // В реальной системе можно отправить критическое уведомление
            }
        }
        initializeDefaultConfigs();

        log.info("===== Finished State Reconciliation =====");
    }

    private void reconcilePositionsForPair(String symbol) {
        log.info("Reconciling position for {}...", symbol);
        Optional<Position> localPositionOpt = positionRepository.findByTradingPairAndIsActive(symbol, true);
        Optional<Position> exchangePositionOpt = exchangeApiService.getExchangePosition(symbol);

        if (localPositionOpt.isPresent() && exchangePositionOpt.isEmpty()) {
            // У нас в БД есть активная позиция, а на бирже ее нет. Это рассинхрон.
            // Вероятно, позиция была закрыта вручную, пока бот был выключен.
            Position localPosition = localPositionOpt.get();
            log.warn("Local active position found for {}, but no corresponding position on exchange. Closing local position.", symbol);
            localPosition.setActive(false);
            localPosition.setCloseTimestamp(LocalDateTime.now());
            // PnL в данном случае рассчитать невозможно, оставляем null или 0.
            positionRepository.save(localPosition);

        } else if (localPositionOpt.isEmpty() && exchangePositionOpt.isPresent()) {
            // На бирже есть позиция, а у нас в БД ее нет. Бот "забыл" о ней.
            // Это самая опасная ситуация.
            Position exchangePosition = exchangePositionOpt.get();
            log.warn("Exchange position found for {}, but no active local position. Creating local position to match exchange state.", symbol);
            // Мы не знаем цену входа, поэтому создаем "аварийную" позицию.
            // Основная цель - взять ее под контроль StopLossService.
            Position emergencyPosition = Position.builder()
                    .tradingPair(exchangePosition.getTradingPair())
                    .quantity(exchangePosition.getQuantity())
                    .isActive(true)
                    .entryPrice(new BigDecimal("0")) // Цена входа неизвестна
                    .side(OrderSide.BUY) // Предполагаем LONG, т.к. спот
                    .build();
            positionRepository.save(emergencyPosition);
            log.warn("An 'emergency' position record has been created. Please verify entry price manually.");

        } else if (localPositionOpt.isPresent() && exchangePositionOpt.isPresent()) {
            // Позиция есть и там, и там. Проверим количество.
            Position localPosition = localPositionOpt.get();
            Position exchangePosition = exchangePositionOpt.get();
            if (localPosition.getQuantity().compareTo(exchangePosition.getQuantity()) != 0) {
                log.warn("Quantity mismatch for {}. Local: {}, Exchange: {}. Updating local quantity.",
                        symbol, localPosition.getQuantity(), exchangePosition.getQuantity());
                localPosition.setQuantity(exchangePosition.getQuantity());
                positionRepository.save(localPosition);
            } else {
                log.info("Position for {} is synchronized. OK.", symbol);
            }
        } else {
            // Позиций нет нигде. Все в порядке.
            log.info("No active positions found for {}. OK.", symbol);
        }
    }

    private void initializeDefaultConfigs() {
        log.info("Initializing default strategy configurations if not present...");
        // Создаем дефолтную конфигурацию для BTCUSDT, если ее еще нет
        if (tradingConfigService.getStrategyConfig("BTCUSDT") == null) {
            StrategyConfigRequest btcConfig = new StrategyConfigRequest(
                    "BTCUSDT",
                    "SMA_CROSSOVER",
                    true,
                    Map.of("shortSmaPeriod", "10", "longSmaPeriod", "50")
            );
            tradingConfigService.updateStrategyConfig(btcConfig);
            log.info("Created default SMA_CROSSOVER config for BTCUSDT.");
        }

        // Создаем дефолтную (но неактивную) конфигурацию для ETHUSDT
        if (tradingConfigService.getStrategyConfig("ETHUSDT") == null) {
            StrategyConfigRequest ethConfig = new StrategyConfigRequest(
                    "ETHUSDT",
                    "BOLLINGER_BANDS",
                    false, // По умолчанию выключена
                    Map.of("period", "20", "stdDevMultiplier", "2.0")
            );
            tradingConfigService.updateStrategyConfig(ethConfig);
            log.info("Created default BOLLINGER_BANDS config for ETHUSDT (inactive).");
        }
    }
}
