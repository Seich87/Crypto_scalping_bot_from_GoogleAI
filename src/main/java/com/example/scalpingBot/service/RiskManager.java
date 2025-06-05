package com.example.scalpingBot.service;

import com.example.scalpingBot.config.RiskManagementConfig;
import com.example.scalpingBot.entity.Position;
import com.example.scalpingBot.entity.RiskEvent;
import com.example.scalpingBot.entity.Trade;
import com.example.scalpingBot.enums.RiskLevel;
import com.example.scalpingBot.exception.RiskManagementException;
import com.example.scalpingBot.repository.PositionRepository;
import com.example.scalpingBot.repository.RiskEventRepository;
import com.example.scalpingBot.repository.TradeRepository;
import com.example.scalpingBot.utils.DateUtils;
import com.example.scalpingBot.utils.MathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Сервис управления рисками для скальпинг-бота
 *
 * Основные функции:
 * - Контроль дневных лимитов потерь (максимум 2%)
 * - Управление размерами позиций (максимум 5% на позицию)
 * - Мониторинг количества позиций (максимум 10 одновременно)
 * - Аварийная остановка при превышении лимитов
 * - Корреляционный анализ между позициями
 * - Динамическая оценка уровня риска
 * - Регистрация всех риск-событий
 *
 * Система работает в реальном времени и может
 * автоматически закрывать позиции при превышении лимитов.
 *
 * @author ScalpingBot Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RiskManager {

    private final RiskManagementConfig riskConfig;
    private final PositionRepository positionRepository;
    private final TradeRepository tradeRepository;
    private final RiskEventRepository riskEventRepository;
    private final NotificationService notificationService;

    /**
     * Кеш для текущих риск-метрик
     */
    private final Map<String, BigDecimal> riskMetricsCache = new ConcurrentHashMap<>();

    /**
     * Флаг аварийной остановки
     */
    private volatile boolean emergencyStopActive = false;

    /**
     * Время последней аварийной остановки
     */
    private volatile LocalDateTime lastEmergencyStop;

    /**
     * Текущий уровень риска системы
     */
    private volatile RiskLevel currentSystemRiskLevel = RiskLevel.MEDIUM;

    /**
     * Проверить, разрешена ли торговля
     *
     * @return true если торговля разрешена
     */
    public boolean isTradingAllowed() {
        try {
            // Проверяем аварийную остановку
            if (isEmergencyStopActive()) {
                return false;
            }

            // Проверяем дневные лимиты
            if (isDailyLossLimitExceeded()) {
                triggerEmergencyStop("Превышен дневной лимит потерь");
                return false;
            }

            // Проверяем системные лимиты
            if (currentSystemRiskLevel == RiskLevel.CRITICAL) {
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("Error checking trading allowance: {}", e.getMessage());
            return false; // В случае ошибки запрещаем торговлю
        }
    }

    /**
     * Проверить, можно ли открыть новую позицию
     *
     * @return true если можно открыть позицию
     */
    public boolean canOpenNewPosition() {
        try {
            if (!isTradingAllowed()) {
                return false;
            }

            // Проверяем количество позиций
            long activePositions = positionRepository.countActivePositions();
            if (activePositions >= riskConfig.getMaxSimultaneousPositions()) {
                log.debug("Position limit reached: {} of {} maximum",
                        activePositions, riskConfig.getMaxSimultaneousPositions());
                return false;
            }

            // Проверяем экспозицию портфеля
            BigDecimal totalExposure = positionRepository.calculateTotalExposure();
            BigDecimal maxExposure = getAvailableBalance().multiply(new BigDecimal("0.8")); // 80% максимум

            if (totalExposure.compareTo(maxExposure) >= 0) {
                log.debug("Portfolio exposure limit reached: {} of {} maximum", totalExposure, maxExposure);
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("Error checking new position allowance: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Валидировать новую позицию перед открытием
     *
     * @param params параметры позиции
     * @return true если позиция валидна
     */
    public boolean validateNewPosition(Object params) {
        try {
            // Здесь должна быть логика валидации параметров позиции
            // В данной реализации возвращаем результат базовых проверок
            return canOpenNewPosition();

        } catch (Exception e) {
            log.error("Error validating new position: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Проверить, нужно ли закрыть позицию
     *
     * @param position позиция для проверки
     * @return true если позицию нужно закрыть
     */
    public boolean shouldClosePosition(Position position) {
        try {
            // Проверяем аварийную остановку
            if (isEmergencyStopActive()) {
                return true;
            }

            // Проверяем индивидуальные лимиты позиции
            if (isPositionRiskExceeded(position)) {
                return true;
            }

            // Проверяем время удержания
            if (position.isExpired()) {
                return true;
            }

            // Проверяем системный уровень риска
            if (currentSystemRiskLevel == RiskLevel.VERY_HIGH && position.isLoss()) {
                return true;
            }

            return false;

        } catch (Exception e) {
            log.error("Error checking position closure for {}: {}", position.getId(), e.getMessage());
            return true; // В случае ошибки закрываем позицию для безопасности
        }
    }

    /**
     * Мониторинг рисков - выполняется каждые 5 секунд
     */
    @Scheduled(fixedRateString = "${scheduler.tasks.risk-monitoring.fixed-rate-seconds:5}000")
    public void monitorRisks() {
        if (!riskConfig.getEmergencyStop().getEnabled()) {
            return;
        }

        try {
            log.debug("Starting risk monitoring cycle");

            // Обновляем метрики риска
            updateRiskMetrics();

            // Проверяем критические условия
            checkCriticalConditions();

            // Обновляем уровень риска системы
            updateSystemRiskLevel();

            // Мониторим позиции
            monitorPositionRisks();

            // Проверяем корреляции
            if (riskConfig.getCorrelation().getEnabled()) {
                checkCorrelationRisks();
            }

            log.debug("Risk monitoring cycle completed. Current risk level: {}", currentSystemRiskLevel);

        } catch (Exception e) {
            log.error("Error in risk monitoring: {}", e.getMessage());

            // Критическая ошибка в системе риск-менеджмента
            createRiskEvent(RiskManagementException.RiskEventType.RISK_SYSTEM_FAILURE,
                    "Risk monitoring system failure: " + e.getMessage(),
                    null, null, null, null, null);
        }
    }

    /**
     * Обновить метрики риска
     */
    private void updateRiskMetrics() {
        try {
            // Дневной P&L
            BigDecimal dailyPnl = calculateDailyPnL();
            riskMetricsCache.put("dailyPnl", dailyPnl);

            // Дневной P&L в процентах
            BigDecimal dailyPnlPercent = calculateDailyPnLPercent();
            riskMetricsCache.put("dailyPnlPercent", dailyPnlPercent);

            // Количество активных позиций
            long activePositions = positionRepository.countActivePositions();
            riskMetricsCache.put("activePositions", new BigDecimal(activePositions));

            // Общая экспозиция
            BigDecimal totalExposure = positionRepository.calculateTotalExposure();
            riskMetricsCache.put("totalExposure", totalExposure);

            // Нереализованный P&L
            BigDecimal unrealizedPnl = positionRepository.calculateTotalUnrealizedPnl();
            riskMetricsCache.put("unrealizedPnl", unrealizedPnl);

            log.debug("Risk metrics updated: Daily P&L: {}%, Active positions: {}, Exposure: {}",
                    dailyPnlPercent, activePositions, totalExposure);

        } catch (Exception e) {
            log.error("Failed to update risk metrics: {}", e.getMessage());
        }
    }

    /**
     * Проверить критические условия
     */
    private void checkCriticalConditions() {
        // Проверяем дневной лимит потерь
        if (isDailyLossLimitExceeded()) {
            BigDecimal currentLoss = riskMetricsCache.get("dailyPnlPercent");
            triggerEmergencyStop(String.format("Превышен дневной лимит потерь: %.2f%%", currentLoss.abs()));
            return;
        }

        // Проверяем лимит позиций
        BigDecimal activePositions = riskMetricsCache.get("activePositions");
        if (activePositions != null && activePositions.intValue() > riskConfig.getMaxSimultaneousPositions()) {
            createRiskEvent(RiskManagementException.RiskEventType.MAX_POSITIONS_LIMIT_BREACH,
                    "Превышен лимит количества позиций",
                    activePositions, new BigDecimal(riskConfig.getMaxSimultaneousPositions()),
                    null, activePositions.intValue(), null);
        }

        // Проверяем серии убытков
        checkConsecutiveLosses();
    }

    /**
     * Проверить серии последовательных убытков
     */
    private void checkConsecutiveLosses() {
        try {
            List<Trade> recentTrades = tradeRepository.findTradesSince(DateUtils.nowMoscow().minusHours(24));

            int consecutiveLosses = 0;
            BigDecimal totalLoss = BigDecimal.ZERO;

            // Подсчитываем последовательные убытки
            for (int i = recentTrades.size() - 1; i >= 0; i--) {
                Trade trade = recentTrades.get(i);
                if (trade.isLoss()) {
                    consecutiveLosses++;
                    totalLoss = totalLoss.add(trade.getRealizedPnl().abs());
                } else {
                    break; // Прерываем серию
                }
            }

            // Проверяем лимит
            if (consecutiveLosses >= riskConfig.getProtection().getMaxConsecutiveLosses()) {
                createRiskEvent(RiskManagementException.RiskEventType.CONSECUTIVE_LOSSES_LIMIT,
                        "Превышен лимит последовательных убытков",
                        new BigDecimal(consecutiveLosses),
                        new BigDecimal(riskConfig.getProtection().getMaxConsecutiveLosses()),
                        null, null, null);
            }

        } catch (Exception e) {
            log.error("Failed to check consecutive losses: {}", e.getMessage());
        }
    }

    /**
     * Обновить системный уровень риска
     */
    private void updateSystemRiskLevel() {
        try {
            BigDecimal dailyPnlPercent = riskMetricsCache.getOrDefault("dailyPnlPercent", BigDecimal.ZERO);
            BigDecimal maxLoss = riskConfig.getMaxDailyLossPercent();

            RiskLevel newLevel = RiskLevel.fromCurrentLoss(dailyPnlPercent, maxLoss);

            if (newLevel != currentSystemRiskLevel) {
                RiskLevel previousLevel = currentSystemRiskLevel;
                currentSystemRiskLevel = newLevel;

                log.info("System risk level changed from {} to {}", previousLevel, newLevel);

                // Создаем событие об изменении уровня риска
                createRiskEvent(RiskManagementException.RiskEventType.RISK_SYSTEM_FAILURE,
                        "Изменение системного уровня риска",
                        dailyPnlPercent, maxLoss, null, null, null);

                // Уведомляем при критических уровнях
                if (newLevel.requiresImmediateAction()) {
                    notificationService.sendCriticalAlert("Risk Level Changed",
                            String.format("System risk level: %s → %s", previousLevel, newLevel));
                }
            }

        } catch (Exception e) {
            log.error("Failed to update system risk level: {}", e.getMessage());
        }
    }

    /**
     * Мониторить риски позиций
     */
    private void monitorPositionRisks() {
        try {
            List<Position> activePositions = positionRepository.findByIsActiveTrueOrderByOpenedAtDesc();

            for (Position position : activePositions) {
                // Проверяем индивидуальные риски позиции
                if (isPositionRiskExceeded(position)) {
                    createRiskEvent(RiskManagementException.RiskEventType.POSITION_LOSS_LIMIT_BREACH,
                            "Превышен лимит потерь по позиции",
                            position.getUnrealizedPnlPercent(),
                            new BigDecimal("-10"), // Пример лимита
                            List.of(position.getTradingPair()), 1, position.getEntryValue());
                }

                // Обновляем уровень риска позиции
                updatePositionRiskLevel(position);
            }

        } catch (Exception e) {
            log.error("Failed to monitor position risks: {}", e.getMessage());
        }
    }

    /**
     * Проверить корреляционные риски
     */
    private void checkCorrelationRisks() {
        try {
            List<Position> activePositions = positionRepository.findByIsActiveTrueOrderByOpenedAtDesc();

            if (activePositions.size() < 2) {
                return; // Нужно минимум 2 позиции для корреляции
            }

            // Группируем позиции по типам пар
            Map<String, List<Position>> positionsByPairType = activePositions.stream()
                    .collect(Collectors.groupingBy(p -> p.getPairType().name()));

            for (Map.Entry<String, List<Position>> entry : positionsByPairType.entrySet()) {
                List<Position> positions = entry.getValue();

                if (positions.size() > 3) { // Слишком много позиций одного типа
                    List<String> pairs = positions.stream()
                            .map(Position::getTradingPair)
                            .collect(Collectors.toList());

                    createRiskEvent(RiskManagementException.RiskEventType.CORRELATION_LIMIT_BREACH,
                            "Высокая концентрация позиций по типу пары",
                            new BigDecimal(positions.size()), new BigDecimal("3"),
                            pairs, positions.size(), null);
                }
            }

        } catch (Exception e) {
            log.error("Failed to check correlation risks: {}", e.getMessage());
        }
    }

    /**
     * Проверить, превышен ли риск позиции
     *
     * @param position позиция
     * @return true если риск превышен
     */
    private boolean isPositionRiskExceeded(Position position) {
        if (position.getUnrealizedPnlPercent() == null) {
            return false;
        }

        // Проверяем процент убытка
        BigDecimal lossPercent = position.getUnrealizedPnlPercent().abs();
        BigDecimal maxLoss = new BigDecimal("10"); // 10% максимальный убыток по позиции

        return lossPercent.compareTo(maxLoss) > 0;
    }

    /**
     * Обновить уровень риска позиции
     *
     * @param position позиция
     */
    private void updatePositionRiskLevel(Position position) {
        try {
            RiskLevel currentLevel = position.getRiskLevel();
            RiskLevel newLevel = calculatePositionRiskLevel(position);

            if (newLevel != currentLevel) {
                position.setRiskLevel(newLevel);
                positionRepository.updateRiskLevel(position.getId(), newLevel);

                log.debug("Position {} risk level changed from {} to {}",
                        position.getId(), currentLevel, newLevel);
            }

        } catch (Exception e) {
            log.error("Failed to update position risk level: {}", e.getMessage());
        }
    }

    /**
     * Рассчитать уровень риска позиции
     *
     * @param position позиция
     * @return уровень риска
     */
    private RiskLevel calculatePositionRiskLevel(Position position) {
        // Базовый уровень на основе времени
        long holdingMinutes = position.getHoldingTimeMinutes();
        RiskLevel timeBasedLevel = RiskLevel.MEDIUM;

        if (holdingMinutes > 45) {
            timeBasedLevel = RiskLevel.HIGH;
        } else if (holdingMinutes > 30) {
            timeBasedLevel = RiskLevel.MEDIUM;
        } else {
            timeBasedLevel = RiskLevel.LOW;
        }

        // Корректируем на основе P&L
        if (position.getUnrealizedPnlPercent() != null) {
            BigDecimal pnlPercent = position.getUnrealizedPnlPercent();

            if (pnlPercent.compareTo(new BigDecimal("-5")) < 0) {
                return RiskLevel.VERY_HIGH;
            } else if (pnlPercent.compareTo(new BigDecimal("-2")) < 0) {
                return RiskLevel.HIGH;
            } else if (pnlPercent.compareTo(new BigDecimal("2")) > 0) {
                return RiskLevel.LOW;
            }
        }

        return timeBasedLevel;
    }

    /**
     * Проверить, превышен ли дневной лимит потерь
     *
     * @return true если лимит превышен
     */
    private boolean isDailyLossLimitExceeded() {
        BigDecimal dailyPnlPercent = riskMetricsCache.get("dailyPnlPercent");
        if (dailyPnlPercent == null) {
            dailyPnlPercent = calculateDailyPnLPercent();
        }

        return dailyPnlPercent.abs().compareTo(riskConfig.getMaxDailyLossPercent()) >= 0;
    }

    /**
     * Рассчитать дневной P&L
     *
     * @return дневной P&L в USDT
     */
    public BigDecimal calculateDailyPnL() {
        BigDecimal realizedPnl = tradeRepository.calculateDailyPnl();
        BigDecimal unrealizedPnl = positionRepository.calculateTotalUnrealizedPnl();

        return realizedPnl.add(unrealizedPnl);
    }

    /**
     * Рассчитать дневной P&L в процентах
     *
     * @return дневной P&L в процентах от баланса
     */
    public BigDecimal calculateDailyPnLPercent() {
        BigDecimal dailyPnl = calculateDailyPnL();
        BigDecimal balance = getAvailableBalance();

        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return dailyPnl.divide(balance, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    /**
     * Получить доступный баланс
     *
     * @return доступный баланс в USDT
     */
    public BigDecimal getAvailableBalance() {
        // Здесь должна быть логика получения баланса от биржи
        // Для демонстрации возвращаем фиксированное значение
        return new BigDecimal("10000"); // $10,000 для примера
    }

    /**
     * Получить максимальный размер позиции в процентах
     *
     * @return максимальный размер позиции
     */
    public BigDecimal getMaxPositionSizePercent() {
        return riskConfig.getMaxPositionSizePercent();
    }

    /**
     * Запустить аварийную остановку
     *
     * @param reason причина остановки
     */
    public void triggerEmergencyStop(String reason) {
        if (emergencyStopActive) {
            return; // Уже активна
        }

        emergencyStopActive = true;
        lastEmergencyStop = DateUtils.nowMoscow();

        log.error("🚨 EMERGENCY STOP TRIGGERED: {}", reason);

        try {
            // Создаем критическое риск-событие
            createRiskEvent(RiskManagementException.RiskEventType.EMERGENCY_STOP_TRIGGERED,
                    reason, null, null, null, null, null);

            // Отправляем критическое уведомление
            notificationService.sendCriticalAlert("🚨 EMERGENCY STOP",
                    String.format("Trading halted: %s", reason));

            // Здесь должна быть логика закрытия всех позиций
            // Это будет делать TradingService

        } catch (Exception e) {
            log.error("Error during emergency stop: {}", e.getMessage());
        }
    }

    /**
     * Проверить, активна ли аварийная остановка
     *
     * @return true если активна
     */
    public boolean isEmergencyStopActive() {
        if (!emergencyStopActive) {
            return false;
        }

        // Проверяем время остывания
        if (lastEmergencyStop != null && riskConfig.getEmergencyStop().getCooldownMinutes() > 0) {
            LocalDateTime cooldownEnd = lastEmergencyStop.plusMinutes(riskConfig.getEmergencyStop().getCooldownMinutes());

            if (DateUtils.nowMoscow().isAfter(cooldownEnd)) {
                if (riskConfig.getEmergencyStop().getAutoRestart()) {
                    deactivateEmergencyStop();
                    return false;
                }
            }
        }

        return emergencyStopActive;
    }

    /**
     * Деактивировать аварийную остановку
     */
    public void deactivateEmergencyStop() {
        emergencyStopActive = false;
        currentSystemRiskLevel = RiskLevel.MEDIUM;

        log.info("Emergency stop deactivated");
        notificationService.sendInfoAlert("Emergency Stop Deactivated", "Trading can be resumed");
    }

    /**
     * Создать риск-событие
     */
    private void createRiskEvent(RiskManagementException.RiskEventType eventType, String description,
                                 BigDecimal currentValue, BigDecimal thresholdValue,
                                 List<String> affectedPairs, Integer affectedPositions,
                                 BigDecimal portfolioExposure) {
        try {
            RiskEvent riskEvent = RiskEvent.builder()
                    .eventType(eventType)
                    .severityLevel(eventType.getSeverity())
                    .isCritical(eventType.getSeverity() >= 4)
                    .requiresImmediateAction(eventType.isRequiresAction())
                    .autoClosePositions(eventType.isAutoClose())
                    .title(eventType.getDescription())
                    .description(description)
                    .currentRiskLevel(currentSystemRiskLevel)
                    .currentValue(currentValue)
                    .thresholdValue(thresholdValue)
                    .affectedPositionsCount(affectedPositions)
                    .affectedPairs(affectedPairs != null ? String.join(",", affectedPairs) : null)
                    .portfolioExposure(portfolioExposure)
                    .accountBalance(getAvailableBalance())
                    .dailyPnl(riskMetricsCache.get("dailyPnl"))
                    .dailyPnlPercent(riskMetricsCache.get("dailyPnlPercent"))
                    .timestamp(DateUtils.nowMoscow())
                    .autoGenerated(true)
                    .source("RiskManager")
                    .component("RiskMonitoring")
                    .build();

            riskEventRepository.save(riskEvent);

            // Отправляем уведомление для критических событий
            if (riskEvent.getIsCritical()) {
                notificationService.sendCriticalAlert("Risk Event",
                        String.format("%s: %s", eventType.getDescription(), description));
            }

        } catch (Exception e) {
            log.error("Failed to create risk event: {}", e.getMessage());
        }
    }

    /**
     * Получить текущий уровень риска системы
     *
     * @return текущий уровень риска
     */
    public RiskLevel getCurrentSystemRiskLevel() {
        return currentSystemRiskLevel;
    }

    /**
     * Получить статистику рисков
     *
     * @return карта с метриками рисков
     */
    public Map<String, Object> getRiskStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("systemRiskLevel", currentSystemRiskLevel);
        stats.put("emergencyStopActive", emergencyStopActive);
        stats.put("dailyPnlPercent", riskMetricsCache.get("dailyPnlPercent"));
        stats.put("activePositions", riskMetricsCache.get("activePositions"));
        stats.put("totalExposure", riskMetricsCache.get("totalExposure"));
        stats.put("maxDailyLoss", riskConfig.getMaxDailyLossPercent());
        stats.put("maxPositions", riskConfig.getMaxSimultaneousPositions());

        return stats;
    }

    /**
     * Сброс дневной статистики в полночь
     */
    @Scheduled(cron = "${scheduler.tasks.daily-reset.cron:0 0 0 * * ?}")
    public void resetDailyStatistics() {
        log.info("Resetting daily risk statistics");

        try {
            // Очищаем кеш дневных метрик
            riskMetricsCache.remove("dailyPnl");
            riskMetricsCache.remove("dailyPnlPercent");

            // Сбрасываем уровень риска
            currentSystemRiskLevel = RiskLevel.MEDIUM;

            // Деактивируем аварийную остановку если она была активна
            if (emergencyStopActive && riskConfig.getEmergencyStop().getAutoRestart()) {
                deactivateEmergencyStop();
            }

            notificationService.sendInfoAlert("Daily Reset", "Risk statistics reset for new trading day");

        } catch (Exception e) {
            log.error("Failed to reset daily statistics: {}", e.getMessage());
        }
    }
}