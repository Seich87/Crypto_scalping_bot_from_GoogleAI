package com.example.scalpingBot.entity;

import com.example.scalpingBot.enums.RiskLevel;
import com.example.scalpingBot.exception.RiskManagementException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Сущность риск-события для скальпинг-бота
 *
 * Регистрирует все критические события системы управления рисками:
 * - Превышение лимитов потерь и позиций
 * - Аварийные остановки торговли
 * - Нарушения корреляционных ограничений
 * - Системные сбои и аномалии
 * - Изменения уровней риска
 *
 * Используется для:
 * - Аудита работы системы риск-менеджмента
 * - Анализа причин потерь и сбоев
 * - Оптимизации параметров риска
 * - Соответствия регуляторным требованиям
 * - Улучшения алгоритмов контроля рисков
 *
 * @author ScalpingBot Team
 * @version 1.0
 */
@Entity
@Table(name = "risk_events", indexes = {
        @Index(name = "idx_risk_event_timestamp", columnList = "timestamp"),
        @Index(name = "idx_risk_event_type", columnList = "eventType"),
        @Index(name = "idx_risk_event_severity", columnList = "severityLevel"),
        @Index(name = "idx_risk_event_pair", columnList = "tradingPair"),
        @Index(name = "idx_risk_event_resolved", columnList = "isResolved"),
        @Index(name = "idx_risk_event_critical", columnList = "isCritical")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskEvent {

    /**
     * Уникальный идентификатор риск-события
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Тип риск-события
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private RiskManagementException.RiskEventType eventType;

    /**
     * Уровень критичности (1-5, где 5 = критический)
     */
    @Column(name = "severity_level", nullable = false)
    private Integer severityLevel;

    /**
     * Является ли событие критическим
     */
    @Column(name = "is_critical", nullable = false)
    private Boolean isCritical;

    /**
     * Требует ли немедленного вмешательства
     */
    @Column(name = "requires_immediate_action", nullable = false)
    private Boolean requiresImmediateAction;

    /**
     * Нужно ли автоматически закрыть позиции
     */
    @Column(name = "auto_close_positions", nullable = false)
    private Boolean autoClosePositions;

    /**
     * Заголовок события
     */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * Подробное описание события
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Торговая пара (если применимо)
     */
    @Column(name = "trading_pair", length = 20)
    private String tradingPair;

    /**
     * Текущий уровень риска
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "current_risk_level", length = 20)
    private RiskLevel currentRiskLevel;

    /**
     * Предыдущий уровень риска
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_risk_level", length = 20)
    private RiskLevel previousRiskLevel;

    /**
     * Текущее значение метрики риска
     */
    @Column(name = "current_value", precision = 18, scale = 8)
    private BigDecimal currentValue;

    /**
     * Пороговое значение
     */
    @Column(name = "threshold_value", precision = 18, scale = 8)
    private BigDecimal thresholdValue;

    /**
     * Превышение порога
     */
    @Column(name = "excess_value", precision = 18, scale = 8)
    private BigDecimal excessValue;

    /**
     * Превышение порога в процентах
     */
    @Column(name = "excess_percent", precision = 8, scale = 4)
    private BigDecimal excessPercent;

    /**
     * Количество затронутых позиций
     */
    @Column(name = "affected_positions_count")
    private Integer affectedPositionsCount;

    /**
     * Список затронутых торговых пар (через запятую)
     */
    @Column(name = "affected_pairs", columnDefinition = "TEXT")
    private String affectedPairs;

    /**
     * Общая экспозиция портфеля на момент события
     */
    @Column(name = "portfolio_exposure", precision = 18, scale = 2)
    private BigDecimal portfolioExposure;

    /**
     * Текущий баланс счета
     */
    @Column(name = "account_balance", precision = 18, scale = 2)
    private BigDecimal accountBalance;

    /**
     * Дневные потери на момент события
     */
    @Column(name = "daily_pnl", precision = 18, scale = 2)
    private BigDecimal dailyPnl;

    /**
     * Дневные потери в процентах
     */
    @Column(name = "daily_pnl_percent", precision = 8, scale = 4)
    private BigDecimal dailyPnlPercent;

    /**
     * Время возникновения события
     */
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    /**
     * Время обнаружения события системой
     */
    @Column(name = "detected_at")
    private LocalDateTime detectedAt;

    /**
     * Время начала обработки события
     */
    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    /**
     * Время завершения обработки события
     */
    @Column(name = "processing_completed_at")
    private LocalDateTime processingCompletedAt;

    /**
     * Время разрешения события
     */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /**
     * Статус события
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EventStatus status;

    /**
     * Решено ли событие
     */
    @Column(name = "is_resolved", nullable = false)
    private Boolean isResolved;

    /**
     * Предпринятые действия
     */
    @Column(name = "actions_taken", columnDefinition = "TEXT")
    private String actionsTaken;

    /**
     * Результат предпринятых действий
     */
    @Column(name = "action_result", columnDefinition = "TEXT")
    private String actionResult;

    /**
     * Рекомендуемые действия
     */
    @Column(name = "recommended_actions", columnDefinition = "TEXT")
    private String recommendedActions;

    /**
     * Причина возникновения события
     */
    @Column(name = "root_cause", columnDefinition = "TEXT")
    private String rootCause;

    /**
     * Было ли отправлено уведомление
     */
    @Column(name = "notification_sent", nullable = false)
    private Boolean notificationSent;

    /**
     * Время отправки уведомления
     */
    @Column(name = "notification_sent_at")
    private LocalDateTime notificationSentAt;

    /**
     * Каналы уведомлений (email, telegram, etc.)
     */
    @Column(name = "notification_channels", length = 200)
    private String notificationChannels;

    /**
     * Автоматически ли создано событие
     */
    @Column(name = "auto_generated", nullable = false)
    private Boolean autoGenerated;

    /**
     * Источник события (система, пользователь, внешний API)
     */
    @Column(name = "source", length = 50)
    private String source;

    /**
     * Компонент системы, сгенерировавший событие
     */
    @Column(name = "component", length = 100)
    private String component;

    /**
     * Дополнительные метаданные в JSON формате
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * Комментарии оператора
     */
    @Column(name = "operator_notes", columnDefinition = "TEXT")
    private String operatorNotes;

    /**
     * Версия для оптимистичной блокировки
     */
    @Version
    private Long version;

    /**
     * Время создания записи
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Время последнего обновления записи
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Статусы обработки риск-событий
     */
    public enum EventStatus {
        NEW,           // Новое событие
        DETECTED,      // Обнаружено системой
        PROCESSING,    // Обрабатывается
        ACTION_TAKEN,  // Действия предприняты
        MONITORING,    // Под мониторингом
        RESOLVED,      // Разрешено
        ESCALATED,     // Эскалировано
        FAILED         // Ошибка обработки
    }

    /**
     * Автоматические обновления временных меток
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Инициализация при создании
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.timestamp == null) {
            this.timestamp = now;
        }
        if (this.detectedAt == null) {
            this.detectedAt = now;
        }
        if (this.status == null) {
            this.status = EventStatus.NEW;
        }
        if (this.isResolved == null) {
            this.isResolved = false;
        }
        if (this.notificationSent == null) {
            this.notificationSent = false;
        }
        if (this.autoGenerated == null) {
            this.autoGenerated = true;
        }

        // Рассчитываем превышение если есть значения
        if (this.currentValue != null && this.thresholdValue != null) {
            this.excessValue = this.currentValue.subtract(this.thresholdValue);
            if (this.thresholdValue.compareTo(BigDecimal.ZERO) > 0) {
                this.excessPercent = this.excessValue.divide(this.thresholdValue, 4, BigDecimal.ROUND_HALF_UP)
                        .multiply(new BigDecimal("100"));
            }
        }
    }

    /**
     * Начать обработку события
     */
    public void startProcessing() {
        this.status = EventStatus.PROCESSING;
        this.processingStartedAt = LocalDateTime.now();
    }

    /**
     * Завершить обработку события
     */
    public void completeProcessing(String actionsTaken, String result) {
        this.status = EventStatus.ACTION_TAKEN;
        this.processingCompletedAt = LocalDateTime.now();
        this.actionsTaken = actionsTaken;
        this.actionResult = result;
    }

    /**
     * Разрешить событие
     */
    public void resolve(String resolution) {
        this.status = EventStatus.RESOLVED;
        this.isResolved = true;
        this.resolvedAt = LocalDateTime.now();
        this.actionResult = resolution;
    }

    /**
     * Эскалировать событие
     */
    public void escalate(String reason) {
        this.status = EventStatus.ESCALATED;
        this.operatorNotes = (this.operatorNotes != null ? this.operatorNotes + "\n" : "") +
                "ESCALATED: " + reason + " at " + LocalDateTime.now();
    }

    /**
     * Отметить отправку уведомления
     */
    public void markNotificationSent(String channels) {
        this.notificationSent = true;
        this.notificationSentAt = LocalDateTime.now();
        this.notificationChannels = channels;
    }

    /**
     * Получить длительность обработки события в минутах
     */
    public Long getProcessingDurationMinutes() {
        if (processingStartedAt == null) {
            return null;
        }

        LocalDateTime endTime = processingCompletedAt != null ?
                processingCompletedAt : LocalDateTime.now();

        return java.time.Duration.between(processingStartedAt, endTime).toMinutes();
    }

    /**
     * Получить время до разрешения в минутах
     */
    public Long getResolutionTimeMinutes() {
        if (resolvedAt == null) {
            return null;
        }

        return java.time.Duration.between(timestamp, resolvedAt).toMinutes();
    }

    /**
     * Проверить, просрочено ли событие
     */
    public boolean isOverdue(int maxProcessingMinutes) {
        if (isResolved) {
            return false;
        }

        LocalDateTime deadline = timestamp.plusMinutes(maxProcessingMinutes);
        return LocalDateTime.now().isAfter(deadline);
    }

    /**
     * Получить приоритет события для обработки
     */
    public int getProcessingPriority() {
        int priority = severityLevel != null ? severityLevel : 1;

        // Увеличиваем приоритет для критических событий
        if (isCritical != null && isCritical) {
            priority += 2;
        }

        // Увеличиваем приоритет для событий, требующих немедленных действий
        if (requiresImmediateAction != null && requiresImmediateAction) {
            priority += 1;
        }

        return Math.min(priority, 10); // Максимальный приоритет 10
    }

    /**
     * Получить цветовой код для UI
     */
    public String getColorCode() {
        if (severityLevel == null) {
            return "#808080"; // Серый
        }

        switch (severityLevel) {
            case 1:
                return "#00FF00"; // Зеленый
            case 2:
                return "#FFFF00"; // Желтый
            case 3:
                return "#FFA500"; // Оранжевый
            case 4:
                return "#FF6600"; // Темно-оранжевый
            case 5:
                return "#FF0000"; // Красный
            default:
                return "#808080"; // Серый
        }
    }

    /**
     * Проверить, связано ли событие с конкретной торговой парой
     */
    public boolean isRelatedToPair(String pair) {
        if (tradingPair != null && tradingPair.equals(pair)) {
            return true;
        }

        if (affectedPairs != null) {
            return affectedPairs.contains(pair);
        }

        return false;
    }

    /**
     * Получить краткое описание события
     */
    public String getShortDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append(eventType != null ? eventType.getDescription() : "Unknown event");

        if (tradingPair != null) {
            desc.append(" (").append(tradingPair).append(")");
        }

        if (currentValue != null && thresholdValue != null) {
            desc.append(" - ").append(currentValue).append(" > ").append(thresholdValue);
        }

        return desc.toString();
    }

    /**
     * Создать событие из исключения
     */
    public static RiskEvent fromException(RiskManagementException ex) {
        return RiskEvent.builder()
                .eventType(ex.getEventType())
                .severityLevel(ex.getSeverityLevel())
                .isCritical(ex.getSeverityLevel() >= 4)
                .requiresImmediateAction(ex.isRequiresImmediateAction())
                .autoClosePositions(ex.isAutoClosePositions())
                .title(ex.getEventType().getDescription())
                .description(ex.getMessage())
                .tradingPair(ex.getAffectedPairs() != null && !ex.getAffectedPairs().isEmpty() ?
                        ex.getAffectedPairs().get(0) : null)
                .currentRiskLevel(ex.getCurrentRiskLevel())
                .previousRiskLevel(ex.getPreviousRiskLevel())
                .currentValue(ex.getCurrentValue())
                .thresholdValue(ex.getThresholdValue())
                .affectedPositionsCount(ex.getAffectedPositions())
                .affectedPairs(ex.getAffectedPairs() != null ?
                        String.join(",", ex.getAffectedPairs()) : null)
                .portfolioExposure(ex.getPortfolioExposure())
                .timestamp(ex.getTimestamp())
                .recommendedActions(ex.getRecommendedActions())
                .autoGenerated(true)
                .source("RiskManagementSystem")
                .component("RiskManager")
                .build();
    }

    @Override
    public String toString() {
        return String.format("RiskEvent{id=%d, type=%s, severity=%d, pair='%s', status=%s, resolved=%s, time=%s}",
                id,
                eventType != null ? eventType.name() : "UNKNOWN",
                severityLevel != null ? severityLevel : 0,
                tradingPair != null ? tradingPair : "ALL",
                status,
                isResolved,
                timestamp);
    }
}