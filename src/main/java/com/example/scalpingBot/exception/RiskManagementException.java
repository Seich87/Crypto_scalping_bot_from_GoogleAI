package com.example.scalpingBot.exception;

import com.example.scalpingBot.enums.RiskLevel;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Исключение для системы управления рисками скальпинг-бота
 *
 * Покрывает критические ситуации:
 * - Превышение лимитов потерь (дневных, позиционных)
 * - Нарушение корреляционных ограничений
 * - Аварийные остановки торговли
 * - Превышение волатильности и экспозиции
 * - Системные сбои риск-контроля
 *
 * @author ScalpingBot Team
 * @version 1.0
 */
@Getter
public class RiskManagementException extends RuntimeException {

    /**
     * Тип риск-события
     */
    private final RiskEventType eventType;

    /**
     * Текущий уровень риска
     */
    private final RiskLevel currentRiskLevel;

    /**
     * Предыдущий уровень риска
     */
    private final RiskLevel previousRiskLevel;

    /**
     * Текущее значение метрики риска
     */
    private final BigDecimal currentValue;

    /**
     * Пороговое значение
     */
    private final BigDecimal thresholdValue;

    /**
     * Затронутые торговые пары
     */
    private final List<String> affectedPairs;

    /**
     * Количество затронутых позиций
     */
    private final Integer affectedPositions;

    /**
     * Общая экспозиция портфеля
     */
    private final BigDecimal portfolioExposure;

    /**
     * Время возникновения события
     */
    private final LocalDateTime timestamp;

    /**
     * Требует ли немедленного вмешательства
     */
    private final boolean requiresImmediateAction;

    /**
     * Нужно ли автоматически закрыть позиции
     */
    private final boolean autoClosePositions;

    /**
     * Перечисление типов риск-событий
     */
    @Getter
    public enum RiskEventType {
        // Лимиты потерь
        DAILY_LOSS_LIMIT_BREACH(
                "Превышен дневной лимит потерь",
                true, true, 5
        ),
        POSITION_LOSS_LIMIT_BREACH(
                "Превышен лимит потерь по позиции",
                true, false, 4
        ),
        PORTFOLIO_DRAWDOWN_LIMIT_BREACH(
                "Превышен лимит просадки портфеля",
                true, true, 5
        ),

        // Лимиты экспозиции
        POSITION_SIZE_LIMIT_BREACH(
                "Превышен лимит размера позиции",
                true, false, 3
        ),
        PORTFOLIO_EXPOSURE_LIMIT_BREACH(
                "Превышен лимит экспозиции портфеля",
                true, true, 4
        ),
        MAX_POSITIONS_LIMIT_BREACH(
                "Превышен лимит количества позиций",
                false, false, 2
        ),

        // Корреляционные риски
        CORRELATION_LIMIT_BREACH(
                "Превышен лимит корреляции",
                true, false, 3
        ),
        SECTOR_CONCENTRATION_RISK(
                "Риск концентрации по секторам",
                false, false, 2
        ),

        // Волатильность
        VOLATILITY_SPIKE(
                "Резкий рост волатильности",
                true, false, 3
        ),
        EXTREME_VOLATILITY(
                "Экстремальная волатильность",
                true, true, 4
        ),

        // Ликвидность
        LIQUIDITY_CRISIS(
                "Кризис ликвидности",
                true, true, 4
        ),
        HIGH_SLIPPAGE_RISK(
                "Высокий риск проскальзывания",
                true, false, 3
        ),

        // Последовательные потери
        CONSECUTIVE_LOSSES_LIMIT(
                "Превышен лимит последовательных потерь",
                true, false, 3
        ),
        LOSING_STREAK_WARNING(
                "Предупреждение о серии убытков",
                false, false, 2
        ),

        // Системные риски
        EMERGENCY_STOP_TRIGGERED(
                "Активирована аварийная остановка",
                true, true, 5
        ),
        RISK_SYSTEM_FAILURE(
                "Сбой системы управления рисками",
                true, true, 5
        ),
        DATA_INTEGRITY_BREACH(
                "Нарушение целостности данных",
                true, true, 5
        ),

        // Балансовые риски
        INSUFFICIENT_MARGIN(
                "Недостаточная маржа",
                true, true, 4
        ),
        BALANCE_THRESHOLD_BREACH(
                "Нарушен минимальный баланс",
                true, false, 3
        ),

        // Временные ограничения
        POSITION_TIME_LIMIT_BREACH(
                "Превышено время удержания позиции",
                true, false, 2
        ),
        TRADING_HOURS_VIOLATION(
                "Нарушение торговых часов",
                false, false, 1
        );

        private final String description;
        private final boolean requiresAction;
        private final boolean autoClose;
        private final int severity; // 1-5, где 5 = критический

        RiskEventType(String description, boolean requiresAction,
                      boolean autoClose, int severity) {
            this.description = description;
            this.requiresAction = requiresAction;
            this.autoClose = autoClose;
            this.severity = severity;
        }
    }

    /**
     * Конструктор с полными параметрами
     */
    public RiskManagementException(RiskEventType eventType, String message,
                                   RiskLevel currentRiskLevel, RiskLevel previousRiskLevel,
                                   BigDecimal currentValue, BigDecimal thresholdValue,
                                   List<String> affectedPairs, Integer affectedPositions,
                                   BigDecimal portfolioExposure) {
        super(formatMessage(eventType, message, currentValue, thresholdValue));
        this.eventType = eventType;
        this.currentRiskLevel = currentRiskLevel;
        this.previousRiskLevel = previousRiskLevel;
        this.currentValue = currentValue;
        this.thresholdValue = thresholdValue;
        this.affectedPairs = affectedPairs;
        this.affectedPositions = affectedPositions;
        this.portfolioExposure = portfolioExposure;
        this.timestamp = LocalDateTime.now();
        this.requiresImmediateAction = eventType.isRequiresAction();
        this.autoClosePositions = eventType.isAutoClose();
    }

    /**
     * Упрощенный конструктор
     */
    public RiskManagementException(RiskEventType eventType, String message,
                                   BigDecimal currentValue, BigDecimal thresholdValue) {
        this(eventType, message, null, null, currentValue, thresholdValue,
                null, null, null);
    }

    /**
     * Конструктор с причиной
     */
    public RiskManagementException(RiskEventType eventType, String message, Throwable cause) {
        super(formatMessage(eventType, message, null, null), cause);
        this.eventType = eventType;
        this.currentRiskLevel = null;
        this.previousRiskLevel = null;
        this.currentValue = null;
        this.thresholdValue = null;
        this.affectedPairs = null;
        this.affectedPositions = null;
        this.portfolioExposure = null;
        this.timestamp = LocalDateTime.now();
        this.requiresImmediateAction = eventType.isRequiresAction();
        this.autoClosePositions = eventType.isAutoClose();
    }

    /**
     * Статические методы для создания типичных исключений
     */

    /**
     * Превышен дневной лимит потерь
     */
    public static RiskManagementException dailyLossLimitBreached(BigDecimal currentLoss,
                                                                 BigDecimal dailyLimit,
                                                                 RiskLevel riskLevel) {
        String message = String.format("Дневные потери %.2f%% превысили лимит %.2f%%",
                currentLoss, dailyLimit);
        return new RiskManagementException(
                RiskEventType.DAILY_LOSS_LIMIT_BREACH, message,
                riskLevel, null, currentLoss, dailyLimit, null, null, null
        );
    }

    /**
     * Превышен лимит размера позиции
     */
    public static RiskManagementException positionSizeLimitBreached(String tradingPair,
                                                                    BigDecimal currentSize,
                                                                    BigDecimal maxSize) {
        String message = String.format("Размер позиции %s превышает лимит", tradingPair);
        return new RiskManagementException(
                RiskEventType.POSITION_SIZE_LIMIT_BREACH, message,
                null, null, currentSize, maxSize, List.of(tradingPair), 1, null
        );
    }

    /**
     * Превышен лимит корреляции
     */
    public static RiskManagementException correlationLimitBreached(List<String> correlatedPairs,
                                                                   BigDecimal correlation,
                                                                   BigDecimal maxCorrelation) {
        String message = String.format("Корреляция %.2f превышает лимит %.2f для пар: %s",
                correlation, maxCorrelation,
                String.join(", ", correlatedPairs));
        return new RiskManagementException(
                RiskEventType.CORRELATION_LIMIT_BREACH, message,
                null, null, correlation, maxCorrelation, correlatedPairs,
                correlatedPairs.size(), null
        );
    }

    /**
     * Аварийная остановка
     */
    public static RiskManagementException emergencyStopTriggered(String reason,
                                                                 RiskLevel currentLevel,
                                                                 BigDecimal currentLoss,
                                                                 BigDecimal threshold,
                                                                 Integer openPositions) {
        String message = String.format("Аварийная остановка: %s. Потери: %.2f%%, порог: %.2f%%",
                reason, currentLoss, threshold);
        return new RiskManagementException(
                RiskEventType.EMERGENCY_STOP_TRIGGERED, message,
                currentLevel, null, currentLoss, threshold, null, openPositions, null
        );
    }

    /**
     * Экстремальная волатильность
     */
    public static RiskManagementException extremeVolatility(String tradingPair,
                                                            BigDecimal currentATR,
                                                            BigDecimal maxATR,
                                                            RiskLevel riskLevel) {
        String message = String.format("Экстремальная волатильность %s: ATR %.2f%% > %.2f%%",
                tradingPair, currentATR, maxATR);
        return new RiskManagementException(
                RiskEventType.EXTREME_VOLATILITY, message,
                riskLevel, null, currentATR, maxATR, List.of(tradingPair), null, null
        );
    }

    /**
     * Превышено количество позиций
     */
    public static RiskManagementException maxPositionsBreached(Integer currentPositions,
                                                               Integer maxPositions) {
        String message = String.format("Открыто %d позиций, максимум: %d",
                currentPositions, maxPositions);
        return new RiskManagementException(
                RiskEventType.MAX_POSITIONS_LIMIT_BREACH, message,
                null, null, new BigDecimal(currentPositions), new BigDecimal(maxPositions),
                null, currentPositions, null
        );
    }

    /**
     * Серия последовательных убытков
     */
    public static RiskManagementException consecutiveLossesLimit(Integer consecutiveLosses,
                                                                 Integer maxLosses,
                                                                 BigDecimal totalLoss) {
        String message = String.format("Последовательных убытков: %d (макс: %d), общие потери: %.2f%%",
                consecutiveLosses, maxLosses, totalLoss);
        return new RiskManagementException(
                RiskEventType.CONSECUTIVE_LOSSES_LIMIT, message,
                null, null, new BigDecimal(consecutiveLosses), new BigDecimal(maxLosses),
                null, null, null
        );
    }

    /**
     * Недостаточная маржа
     */
    public static RiskManagementException insufficientMargin(BigDecimal requiredMargin,
                                                             BigDecimal availableMargin,
                                                             BigDecimal portfolioValue) {
        String message = String.format("Требуется маржа: %.2f, доступно: %.2f",
                requiredMargin, availableMargin);
        return new RiskManagementException(
                RiskEventType.INSUFFICIENT_MARGIN, message,
                null, null, requiredMargin, availableMargin, null, null, portfolioValue
        );
    }

    /**
     * Сбой системы управления рисками
     */
    public static RiskManagementException systemFailure(String systemComponent, Throwable cause) {
        String message = String.format("Сбой компонента: %s", systemComponent);
        return new RiskManagementException(RiskEventType.RISK_SYSTEM_FAILURE, message, cause);
    }

    /**
     * Форматирование сообщения
     */
    private static String formatMessage(RiskEventType eventType, String message,
                                        BigDecimal currentValue, BigDecimal thresholdValue) {
        StringBuilder sb = new StringBuilder();
        sb.append("[RISK-").append(eventType.getSeverity()).append("] ");
        sb.append(eventType.getDescription());

        if (message != null && !message.isEmpty()) {
            sb.append(": ").append(message);
        }

        if (currentValue != null && thresholdValue != null) {
            sb.append(" [").append(currentValue).append(" > ").append(thresholdValue).append("]");
        }

        return sb.toString();
    }

    /**
     * Получить код события для логирования
     */
    public String getEventCode() {
        return eventType.name();
    }

    /**
     * Получить описание события
     */
    public String getEventDescription() {
        return eventType.getDescription();
    }

    /**
     * Получить уровень критичности (1-5)
     */
    public int getSeverityLevel() {
        return eventType.getSeverity();
    }

    /**
     * Получить рекомендуемые действия
     */
    public String getRecommendedActions() {
        switch (eventType) {
            case DAILY_LOSS_LIMIT_BREACH:
                return "1. Немедленно закрыть все позиции\n" +
                        "2. Остановить торговлю до следующего дня\n" +
                        "3. Проанализировать причины потерь";

            case EMERGENCY_STOP_TRIGGERED:
                return "1. НЕМЕДЛЕННО остановить все торговые операции\n" +
                        "2. Проверить состояние всех позиций\n" +
                        "3. Уведомить администратора\n" +
                        "4. Провести полный аудит системы";

            case CORRELATION_LIMIT_BREACH:
                return "1. Закрыть наиболее коррелированные позиции\n" +
                        "2. Пересмотреть диверсификацию портфеля\n" +
                        "3. Обновить корреляционную матрицу";

            case EXTREME_VOLATILITY:
                return "1. Снизить размеры позиций\n" +
                        "2. Расширить стоп-лоссы\n" +
                        "3. Увеличить частоту мониторинга\n" +
                        "4. Рассмотреть временную остановку торговли";

            case MAX_POSITIONS_LIMIT_BREACH:
                return "1. Закрыть наименее прибыльные позиции\n" +
                        "2. Дождаться естественного закрытия позиций\n" +
                        "3. Пересмотреть лимиты";

            case CONSECUTIVE_LOSSES_LIMIT:
                return "1. Временно остановить открытие новых позиций\n" +
                        "2. Проанализировать торговую стратегию\n" +
                        "3. Проверить рыночные условия\n" +
                        "4. Рассмотреть снижение размеров позиций";

            case INSUFFICIENT_MARGIN:
                return "1. Закрыть часть позиций для освобождения маржи\n" +
                        "2. Пополнить счет\n" +
                        "3. Пересмотреть размеры позиций";

            case RISK_SYSTEM_FAILURE:
                return "1. НЕМЕДЛЕННО остановить автоматическую торговлю\n" +
                        "2. Перейти на ручное управление рисками\n" +
                        "3. Устранить неисправность системы\n" +
                        "4. Провести тестирование перед возобновлением";

            default:
                return "1. Проанализировать ситуацию\n" +
                        "2. Принять соответствующие меры\n" +
                        "3. Мониторить развитие событий";
        }
    }

    /**
     * Получить приоритет уведомления
     */
    public int getNotificationPriority() {
        return eventType.getSeverity();
    }

    /**
     * Нужно ли немедленное уведомление
     */
    public boolean requiresImmediateNotification() {
        return eventType.getSeverity() >= 4;
    }

    /**
     * Получить время до автоматического закрытия позиций (в секундах)
     */
    public int getAutoCloseTimeoutSeconds() {
        if (!autoClosePositions) {
            return -1;
        }

        switch (eventType) {
            case EMERGENCY_STOP_TRIGGERED:
            case RISK_SYSTEM_FAILURE:
                return 0; // немедленно
            case DAILY_LOSS_LIMIT_BREACH:
            case EXTREME_VOLATILITY:
                return 30; // 30 секунд
            case PORTFOLIO_EXPOSURE_LIMIT_BREACH:
            case LIQUIDITY_CRISIS:
                return 60; // 1 минута
            default:
                return 300; // 5 минут
        }
    }

    /**
     * Создать детальный отчет о риск-событии
     */
    public String getDetailedReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ОТЧЕТ О РИСК-СОБЫТИИ ===\n");
        sb.append("Тип события: ").append(eventType.getDescription()).append("\n");
        sb.append("Критичность: ").append(eventType.getSeverity()).append("/5\n");
        sb.append("Время: ").append(timestamp).append("\n");
        sb.append("Требует действий: ").append(requiresImmediateAction ? "ДА" : "НЕТ").append("\n");
        sb.append("Автозакрытие: ").append(autoClosePositions ? "ДА" : "НЕТ").append("\n");

        if (currentRiskLevel != null) {
            sb.append("Текущий уровень риска: ").append(currentRiskLevel).append("\n");
        }
        if (previousRiskLevel != null) {
            sb.append("Предыдущий уровень риска: ").append(previousRiskLevel).append("\n");
        }
        if (currentValue != null && thresholdValue != null) {
            sb.append("Текущее значение: ").append(currentValue).append("\n");
            sb.append("Пороговое значение: ").append(thresholdValue).append("\n");
            BigDecimal excess = currentValue.subtract(thresholdValue);
            sb.append("Превышение: ").append(excess).append("\n");
        }
        if (affectedPairs != null && !affectedPairs.isEmpty()) {
            sb.append("Затронутые пары: ").append(String.join(", ", affectedPairs)).append("\n");
        }
        if (affectedPositions != null) {
            sb.append("Затронутые позиции: ").append(affectedPositions).append("\n");
        }
        if (portfolioExposure != null) {
            sb.append("Экспозиция портфеля: ").append(portfolioExposure).append("%\n");
        }

        sb.append("\n=== РЕКОМЕНДУЕМЫЕ ДЕЙСТВИЯ ===\n");
        sb.append(getRecommendedActions());

        return sb.toString();
    }
}