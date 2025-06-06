package com.example.scalpingBot.scheduler;

import com.example.scalpingBot.entity.Position;
import com.example.scalpingBot.repository.PositionRepository;
import com.example.scalpingBot.service.notification.NotificationService;
import com.example.scalpingBot.service.risk.StopLossService;
import com.example.scalpingBot.service.risk.TakeProfitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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

    @Autowired
    public RiskMonitorScheduler(PositionRepository positionRepository,
                                StopLossService stopLossService,
                                TakeProfitService takeProfitService,
                                NotificationService notificationService) {
        this.positionRepository = positionRepository;
        this.stopLossService = stopLossService;
        this.takeProfitService = takeProfitService;
        this.notificationService = notificationService;
    }

    /**
     * Этот метод выполняется каждую секунду.
     * fixedRate = 1000 миллисекунд.
     * 'fixedRate' используется здесь, потому что проверки быстрые, и мы хотим
     * гарантировать их регулярность, даже если предыдущая немного задержалась.
     */
    @Scheduled(fixedRate = 1000, initialDelay = 30000)
    public void monitorActivePositions() {
        // 1. Получаем все активные (открытые) позиции из базы данных
        List<Position> activePositions = positionRepository.findAllByIsActive(true);

        if (activePositions.isEmpty()) {
            return; // Если нет активных позиций, ничего не делаем
        }

        log.trace("Monitoring {} active position(s)...", activePositions.size());

        // 2. Для каждой активной позиции запускаем проверки
        for (Position position : activePositions) {
            try {
                // Проверяем, не пора ли закрыться по стоп-лоссу
                stopLossService.checkAndTriggerStopLoss(position);

                // Если позиция все еще активна после проверки SL, проверяем тейк-профит
                // (перезагружаем позицию из БД, т.к. она могла быть изменена в stopLossService)
                Position currentPositionState = positionRepository.findById(position.getId()).orElse(null);
                if (currentPositionState != null && currentPositionState.isActive()) {
                    takeProfitService.checkAndTriggerTakeProfit(currentPositionState);
                }

            } catch (Exception e) {
                log.error("!! CRITICAL ERROR in risk monitoring for position {}: {}", position.getId(), e.getMessage(), e);
                notificationService.sendError("Критическая ошибка в мониторинге рисков для " + position.getTradingPair(), e);
            }
        }
    }
}