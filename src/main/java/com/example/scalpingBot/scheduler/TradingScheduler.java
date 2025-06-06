package com.example.scalpingBot.scheduler;

import com.example.scalpingBot.dto.exchange.TickerDto;
import com.example.scalpingBot.entity.Position;
import com.example.scalpingBot.enums.OrderSide;
import com.example.scalpingBot.service.market.MarketDataService;
import com.example.scalpingBot.service.notification.NotificationService;
import com.example.scalpingBot.service.trading.PositionManager;
import com.example.scalpingBot.service.trading.ScalpingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Основной торговый планировщик.
 * Периодически запускает торговую стратегию и принимает решения об открытии/закрытии позиций.
 */
@Component
public class TradingScheduler {

    private static final Logger log = LoggerFactory.getLogger(TradingScheduler.class);

    private final ScalpingStrategy scalpingStrategy;
    private final PositionManager positionManager;
    private final MarketDataService marketDataService;
    private final NotificationService notificationService;
    private final List<String> tradingPairs;

    @Autowired
    public TradingScheduler(ScalpingStrategy scalpingStrategy,
                            PositionManager positionManager,
                            MarketDataService marketDataService,
                            NotificationService notificationService,
                            @Value("${bot.trading-pairs}") List<String> tradingPairs) {
        this.scalpingStrategy = scalpingStrategy;
        this.positionManager = positionManager;
        this.marketDataService = marketDataService;
        this.notificationService = notificationService;
        this.tradingPairs = tradingPairs;
    }

    /**
     * Этот метод выполняется каждые 15 секунд.
     * fixedDelay = 15000 миллисекунд.
     * 'fixedDelay' означает, что следующий запуск начнется через 15 секунд
     * после *завершения* предыдущего. Это предотвращает наложение задач.
     */
    @Scheduled(fixedDelay = 15000, initialDelay = 20000)
    public void executeTradingLogic() {
        log.info(">>> Starting trading logic cycle <<<");
        for (String pair : tradingPairs) {
            try {
                processTradingPair(pair);
            } catch (Exception e) {
                log.error("!! CRITICAL ERROR in trading logic for pair {}: {}", pair, e.getMessage(), e);
                notificationService.sendError("Критическая ошибка в торговой логике для " + pair, e);
            }
        }
        log.info(">>> Finished trading logic cycle <<<");
    }

    private void processTradingPair(String symbol) {
        log.debug("Processing pair: {}", symbol);

        // 1. Проверяем, есть ли уже активная позиция
        Optional<Position> activePositionOpt = positionManager.getActivePosition(symbol);

        // 2. Генерируем торговый сигнал
        Optional<OrderSide> signalOpt = scalpingStrategy.generateSignal(symbol);

        if (activePositionOpt.isPresent()) {
            // --- Логика для ОТКРЫТОЙ позиции ---
            Position activePosition = activePositionOpt.get();
            if (signalOpt.isPresent() && signalOpt.get() != activePosition.getSide()) {
                // Если есть сигнал на закрытие (противоположный текущей позиции)
                log.info("Closing signal received for active position on {}", symbol);
                TickerDto ticker = marketDataService.getCurrentTicker(symbol);
                positionManager.closePosition(symbol, ticker.getLastPrice());
                notificationService.sendSuccess("Позиция по " + symbol + " закрыта по сигналу стратегии.");
            }
        } else {
            // --- Логика для ОТСУТСТВИЯ позиции ---
            if (signalOpt.isPresent() && signalOpt.get() == OrderSide.BUY) {
                // Если есть сигнал на открытие LONG позиции
                log.info("Opening signal received for {}", symbol);
                TickerDto ticker = marketDataService.getCurrentTicker(symbol);
                positionManager.openPosition(symbol, OrderSide.BUY, ticker.getLastPrice());
                notificationService.sendInfo("Открыта LONG позиция по " + symbol);
            }
            // Здесь можно добавить логику для открытия SHORT позиций, если стратегия их поддерживает
        }
    }
}