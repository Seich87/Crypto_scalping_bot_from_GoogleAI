package com.example.scalpingBot.exception;

import com.example.scalpingBot.enums.OrderStatus;
import com.example.scalpingBot.enums.RiskLevel;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Исключение для торговых операций скальпинг-бота
 *
 * Покрывает все ошибки связанные с:
 * - Размещением и исполнением ордеров
 * - Управлением позициями
 * - Расчетом размеров позиций
 * - Валидацией торговых параметров
 * - Проблемами ликвидности и спредов
 *
 * @author ScalpingBot Team
 * @version 1.0
 */
@Getter
public class TradingException extends RuntimeException {

    /**
     * Тип ошибки торговой операции
     */
    private final TradingErrorType errorType;

    /**
     * Торговая пара, связанная с ошибкой
     */
    private final String tradingPair;

    /**
     * Размер позиции, вызвавший ошибку
     */
    private final BigDecimal positionSize;

    /**
     * Цена, связанная с ошибкой
     */
    private final BigDecimal price;

    /**
     * Статус ордера на момент ошибки
     */
    private final OrderStatus orderStatus;

    /**
     * Уровень риска на момент ошибки
     */
    private final RiskLevel riskLevel;

    /**
     * Время возникновения ошибки
     */
    private final LocalDateTime timestamp;

    /**
     * Можно ли повторить операцию
     */
    private final boolean retryable;

    /**
     * Требует ли ошибка немедленного вмешательства
     */
    private final boolean critical;

    /**
     * Перечисление типов торговых ошибок
     */
    @Getter
    public enum TradingErrorType {
        // Ошибки валидации
        INVALID_POSITION_SIZE("Неверный размер позиции", true, false),
        INVALID_PRICE("Неверная цена", true, false),
        INVALID_TRADING_PAIR("Неверная торговая пара", false, false),

        // Ошибки баланса
        INSUFFICIENT_BALANCE("Недостаточный баланс", true, false),
        INSUFFICIENT_MARGIN("Недостаточная маржа", true, false),

        // Ошибки лимитов
        POSITION_LIMIT_EXCEEDED("Превышен лимит позиций", true, false),
        DAILY_LOSS_LIMIT_EXCEEDED("Превышен дневной лимит потерь", false, true),
        RISK_LIMIT_EXCEEDED("Превышен лимит риска", false, true),

        // Рыночные ошибки
        MARKET_CLOSED("Рынок закрыт", true, false),
        LOW_LIQUIDITY("Низкая ликвидность", true, false),
        HIGH_SPREAD("Высокий спред", true, false),
        PRICE_IMPACT_TOO_HIGH("Слишком высокое влияние на цену", true, false),

        // Ошибки ордеров
        ORDER_REJECTED("Ордер отклонен", true, false),
        ORDER_FAILED("Ошибка исполнения ордера", true, false),
        ORDER_TIMEOUT("Таймаут ордера", true, false),
        ORDER_PARTIALLY_FILLED("Ордер частично исполнен", true, false),

        // Технические ошибки
        EXCHANGE_CONNECTION_ERROR("Ошибка соединения с биржей", true, true),
        API_ERROR("Ошибка API биржи", true, false),
        RATE_LIMIT_EXCEEDED("Превышен лимит запросов", true, false),

        // Стратегические ошибки
        NO_TRADING_SIGNAL("Отсутствует торговый сигнал", false, false),
        CONFLICTING_SIGNALS("Противоречивые сигналы", false, false),
        VOLATILITY_TOO_HIGH("Слишком высокая волатильность", true, false),
        CORRELATION_LIMIT_EXCEEDED("Превышен лимит корреляции", true, false),

        // Критические ошибки
        EMERGENCY_STOP_TRIGGERED("Аварийная остановка", false, true),
        SYSTEM_ERROR("Системная ошибка", false, true),
        DATA_CORRUPTION("Повреждение данных", false, true);

        private final String description;
        private final boolean retryable;
        private final boolean critical;

        TradingErrorType(String description, boolean retryable, boolean critical) {
            this.description = description;
            this.retryable = retryable;
            this.critical = critical;
        }
    }

    /**
     * Конструктор с полными параметрами
     */
    public TradingException(TradingErrorType errorType, String message,
                            String tradingPair, BigDecimal positionSize, BigDecimal price,
                            OrderStatus orderStatus, RiskLevel riskLevel) {
        super(formatMessage(errorType, message, tradingPair));
        this.errorType = errorType;
        this.tradingPair = tradingPair;
        this.positionSize = positionSize;
        this.price = price;
        this.orderStatus = orderStatus;
        this.riskLevel = riskLevel;
        this.timestamp = LocalDateTime.now();
        this.retryable = errorType.isRetryable();
        this.critical = errorType.isCritical();
    }

    /**
     * Конструктор с причиной
     */
    public TradingException(TradingErrorType errorType, String message,
                            String tradingPair, Throwable cause) {
        super(formatMessage(errorType, message, tradingPair), cause);
        this.errorType = errorType;
        this.tradingPair = tradingPair;
        this.positionSize = null;
        this.price = null;
        this.orderStatus = null;
        this.riskLevel = null;
        this.timestamp = LocalDateTime.now();
        this.retryable = errorType.isRetryable();
        this.critical = errorType.isCritical();
    }

    /**
     * Конструктор для простых ошибок
     */
    public TradingException(TradingErrorType errorType, String message) {
        super(formatMessage(errorType, message, null));
        this.errorType = errorType;
        this.tradingPair = null;
        this.positionSize = null;
        this.price = null;
        this.orderStatus = null;
        this.riskLevel = null;
        this.timestamp = LocalDateTime.now();
        this.retryable = errorType.isRetryable();
        this.critical = errorType.isCritical();
    }

    /**
     * Статические методы для создания типичных исключений
     */

    /**
     * Недостаточный баланс
     */
    public static TradingException insufficientBalance(String tradingPair,
                                                       BigDecimal requiredAmount,
                                                       BigDecimal availableAmount) {
        String message = String.format("Требуется: %s, доступно: %s",
                requiredAmount, availableAmount);
        return new TradingException(TradingErrorType.INSUFFICIENT_BALANCE, message,
                tradingPair, requiredAmount, null, null, null);
    }

    /**
     * Превышен лимит позиций
     */
    public static TradingException positionLimitExceeded(int currentPositions, int maxPositions) {
        String message = String.format("Текущие позиции: %d, максимум: %d",
                currentPositions, maxPositions);
        return new TradingException(TradingErrorType.POSITION_LIMIT_EXCEEDED, message);
    }

    /**
     * Превышен дневной лимит потерь
     */
    public static TradingException dailyLossLimitExceeded(BigDecimal currentLoss,
                                                          BigDecimal maxLoss,
                                                          RiskLevel riskLevel) {
        String message = String.format("Текущие потери: %s%%, лимит: %s%%",
                currentLoss, maxLoss);
        return new TradingException(TradingErrorType.DAILY_LOSS_LIMIT_EXCEEDED, message,
                null, null, null, null, riskLevel);
    }

    /**
     * Высокий спред
     */
    public static TradingException highSpread(String tradingPair, BigDecimal currentSpread,
                                              BigDecimal maxSpread) {
        String message = String.format("Текущий спред: %s%%, максимум: %s%%",
                currentSpread, maxSpread);
        return new TradingException(TradingErrorType.HIGH_SPREAD, message, tradingPair,
                null, null, null, null);
    }

    /**
     * Ошибка соединения с биржей
     */
    public static TradingException exchangeConnectionError(String exchangeName, Throwable cause) {
        String message = String.format("Потеряно соединение с биржей: %s", exchangeName);
        return new TradingException(TradingErrorType.EXCHANGE_CONNECTION_ERROR, message,
                null, cause);
    }

    /**
     * Ордер отклонен
     */
    public static TradingException orderRejected(String tradingPair, String reason,
                                                 OrderStatus status) {
        String message = String.format("Ордер отклонен: %s", reason);
        return new TradingException(TradingErrorType.ORDER_REJECTED, message, tradingPair,
                null, null, status, null);
    }

    /**
     * Слишком высокая волатильность
     */
    public static TradingException volatilityTooHigh(String tradingPair, BigDecimal currentATR,
                                                     BigDecimal maxATR, RiskLevel riskLevel) {
        String message = String.format("ATR: %s%%, максимум для %s: %s%%",
                currentATR, riskLevel, maxATR);
        return new TradingException(TradingErrorType.VOLATILITY_TOO_HIGH, message, tradingPair,
                null, null, null, riskLevel);
    }

    /**
     * Аварийная остановка
     */
    public static TradingException emergencyStop(String reason, BigDecimal currentLoss,
                                                 BigDecimal threshold) {
        String message = String.format("Аварийная остановка: %s. Потери: %s%%, порог: %s%%",
                reason, currentLoss, threshold);
        return new TradingException(TradingErrorType.EMERGENCY_STOP_TRIGGERED, message);
    }

    /**
     * Неверный размер позиции
     */
    public static TradingException invalidPositionSize(String tradingPair, BigDecimal size,
                                                       BigDecimal minSize, BigDecimal maxSize) {
        String message = String.format("Размер: %s, допустимый диапазон: %s - %s",
                size, minSize, maxSize);
        return new TradingException(TradingErrorType.INVALID_POSITION_SIZE, message,
                tradingPair, size, null, null, null);
    }

    /**
     * Форматирование сообщения об ошибке
     */
    private static String formatMessage(TradingErrorType errorType, String message,
                                        String tradingPair) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(errorType.name()).append("] ");
        sb.append(errorType.getDescription());

        if (tradingPair != null && !tradingPair.isEmpty()) {
            sb.append(" (").append(tradingPair).append(")");
        }

        if (message != null && !message.isEmpty()) {
            sb.append(": ").append(message);
        }

        return sb.toString();
    }

    /**
     * Получить код ошибки для логирования
     */
    public String getErrorCode() {
        return errorType.name();
    }

    /**
     * Получить описание ошибки
     */
    public String getErrorDescription() {
        return errorType.getDescription();
    }

    /**
     * Получить рекомендуемое действие
     */
    public String getRecommendedAction() {
        switch (errorType) {
            case INSUFFICIENT_BALANCE:
                return "Проверить баланс и пополнить счет";
            case POSITION_LIMIT_EXCEEDED:
                return "Закрыть часть открытых позиций";
            case DAILY_LOSS_LIMIT_EXCEEDED:
                return "Остановить торговлю до следующего дня";
            case HIGH_SPREAD:
                return "Дождаться улучшения рыночных условий";
            case EXCHANGE_CONNECTION_ERROR:
                return "Проверить соединение и переподключиться";
            case ORDER_REJECTED:
                return "Проверить параметры ордера и повторить";
            case VOLATILITY_TOO_HIGH:
                return "Снизить размеры позиций или приостановить торговлю";
            case EMERGENCY_STOP_TRIGGERED:
                return "Немедленно проверить все позиции и риски";
            case SYSTEM_ERROR:
                return "Обратиться к администратору системы";
            default:
                return "Проанализировать причину ошибки";
        }
    }

    /**
     * Получить задержку перед повторной попыткой (в секундах)
     */
    public int getRetryDelaySeconds() {
        if (!retryable) {
            return -1; // повтор невозможен
        }

        switch (errorType) {
            case RATE_LIMIT_EXCEEDED:
                return 60; // 1 минута
            case EXCHANGE_CONNECTION_ERROR:
                return 30; // 30 секунд
            case HIGH_SPREAD:
            case LOW_LIQUIDITY:
                return 120; // 2 минуты
            case ORDER_TIMEOUT:
                return 10; // 10 секунд
            default:
                return 5; // 5 секунд по умолчанию
        }
    }

    /**
     * Проверить, нужно ли отправить уведомление
     */
    public boolean shouldNotify() {
        return critical ||
                errorType == TradingErrorType.DAILY_LOSS_LIMIT_EXCEEDED ||
                errorType == TradingErrorType.EXCHANGE_CONNECTION_ERROR ||
                errorType == TradingErrorType.ORDER_REJECTED;
    }

    /**
     * Получить приоритет уведомления (чем выше число, тем важнее)
     */
    public int getNotificationPriority() {
        if (critical) {
            return 5; // критический
        }

        switch (errorType) {
            case DAILY_LOSS_LIMIT_EXCEEDED:
            case RISK_LIMIT_EXCEEDED:
                return 4; // высокий
            case EXCHANGE_CONNECTION_ERROR:
            case ORDER_REJECTED:
                return 3; // средний
            case INSUFFICIENT_BALANCE:
            case POSITION_LIMIT_EXCEEDED:
                return 2; // низкий
            default:
                return 1; // информационный
        }
    }

    /**
     * Создать детальную информацию об ошибке для логирования
     */
    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Ошибка торговой операции:\n");
        sb.append("Тип: ").append(errorType.getDescription()).append("\n");
        sb.append("Время: ").append(timestamp).append("\n");
        sb.append("Критическая: ").append(critical ? "ДА" : "НЕТ").append("\n");
        sb.append("Можно повторить: ").append(retryable ? "ДА" : "НЕТ").append("\n");

        if (tradingPair != null) {
            sb.append("Торговая пара: ").append(tradingPair).append("\n");
        }
        if (positionSize != null) {
            sb.append("Размер позиции: ").append(positionSize).append("\n");
        }
        if (price != null) {
            sb.append("Цена: ").append(price).append("\n");
        }
        if (orderStatus != null) {
            sb.append("Статус ордера: ").append(orderStatus).append("\n");
        }
        if (riskLevel != null) {
            sb.append("Уровень риска: ").append(riskLevel).append("\n");
        }

        sb.append("Рекомендуемое действие: ").append(getRecommendedAction());

        return sb.toString();
    }
}