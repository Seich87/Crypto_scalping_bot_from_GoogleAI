package com.example.scalpingBot.scheduler;

import com.example.scalpingBot.dto.exchange.TickerDto;
import com.example.scalpingBot.entity.Position;
import com.example.scalpingBot.repository.PositionRepository;
import com.example.scalpingBot.service.market.MarketDataService;
import com.example.scalpingBot.service.notification.NotificationService;
import com.example.scalpingBot.service.risk.StopLossService;
import com.example.scalpingBot.service.risk.TakeProfitService;
import com.example.scalpingBot.service.risk.TrailingStopService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.example.scalpingBot.service.event.MarketDataEvent;
import org.springframework.context.event.EventListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import java.util.List;

/**
 * Планировщик, отвечающий за непрерывный мониторинг рисков по активным позициям.
 * Работает с высокой частотой для своевременного срабатывания стоп-лоссов и тейк-профитов.
 */
@Component
public class RiskMonitorScheduler {

    private static final Logger log = LoggerFactory.getLogger(RiskMonitorScheduler.class);

    private final PositionRepository positionRepository;
    private final StopLossService stopLossService;
    private final TakeProfitService takeProfitService;
    private final NotificationService notificationService;
    private final TrailingStopService trailingStopService;
    private final Map<String, TickerDto> lastTickers = new ConcurrentHashMap<>();

    @Autowired
    public RiskMonitorScheduler(PositionRepository positionRepository,
                                StopLossService stopLossService,
                                TakeProfitService takeProfitService,
                                NotificationService notificationService,
                                TrailingStopService trailingStopService) {
        this.positionRepository = positionRepository;
        this.stopLossService = stopLossService;
        this.takeProfitService = takeProfitService;
        this.notificationService = notificationService;
        this.trailingStopService = trailingStopService;
    }

    // +++ НОВЫЙ МЕТОД-СЛУШАТЕЛЬ +++
    @EventListener(MarketDataEvent.class)
    public void handleMarketDataEvent(MarketDataEvent event) {
        TickerDto ticker = event.getTicker();
        lastTickers.put(ticker.getSymbol(), ticker); // Сохраняем последнее известное значение

        // Ищем, есть ли активная позиция для этой пары
        positionRepository.findByTradingPairAndIsActive(ticker.getSymbol(), true)
                .ifPresent(position -> {
                    log.trace("Event received for active position {}. Price: {}", position.getTradingPair(), ticker.getLastPrice());
                    try {
                        // Логика мониторинга, как и раньше, но теперь она запускается мгновенно
                        trailingStopService.updateTrailingStop(position, ticker.getLastPrice());
                        stopLossService.checkAndTriggerStopLoss(position);

                        Position currentPositionState = positionRepository.findById(position.getId()).orElse(null);
                        if (currentPositionState != null && currentPositionState.isActive()) {
                            takeProfitService.checkAndTriggerTakeProfit(currentPositionState);
                        }
                    } catch (Exception e) {
                        log.error("!! CRITICAL ERROR in event-driven risk monitoring for position {}: {}", position.getId(), e.getMessage(), e);
                        notificationService.sendError("Критическая ошибка в мониторинге рисков для " + position.getTradingPair(), e);
                    }
                });
    }

    // Нам все еще нужен способ получать цену для других сервисов, которые не слушают события
    public TickerDto getLatestTicker(String symbol) {
        return lastTickers.get(symbol);
    }
}