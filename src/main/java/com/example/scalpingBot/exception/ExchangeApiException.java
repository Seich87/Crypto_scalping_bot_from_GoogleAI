package com.example.scalpingBot.exception;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Исключение для ошибок API криптовалютных бирж
 *
 * Покрывает все типы ошибок взаимодействия с биржами:
 * - Ошибки аутентификации и авторизации
 * - Превышение лимитов запросов (rate limits)
 * - Сетевые ошибки и таймауты
 * - Ошибки валидации параметров
 * - Технические проблемы бирж
 * - Проблемы с балансом и маржей
 *
 * @author ScalpingBot Team
 * @version 1.0
 */
@Getter
public class ExchangeApiException extends RuntimeException {

    /**
     * Название биржи
     */
    private final String exchangeName;

    /**
     * Тип ошибки API
     */
    private final ApiErrorType errorType;

    /**
     * HTTP статус код ответа
     */
    private final Integer httpStatusCode;

    /**
     * Код ошибки биржи
     */
    private final String exchangeErrorCode;

    /**
     * Время до следующего разрешенного запроса (для rate limit)
     */
    private final Long retryAfterMs;

    /**
     * Количество оставшихся запросов
     */
    private final Integer remainingRequests;

    /**
     * Лимит запросов в период
     */
    private final Integer requestLimit;

    /**
     * Период лимита в миллисекундах
     */
    private final Long limitPeriodMs;

    /**
     * Дополнительные параметры ошибки
     */
    private final Map<String, Object> errorDetails;

    /**
     * Время возникновения ошибки
     */
    private final LocalDateTime timestamp;

    /**
     * Можно ли автоматически повторить запрос
     */
    private final boolean retryable;

    /**
     * Критическая ли ошибка для торговли
     */
    private final boolean critical;

    /**
     * Перечисление типов ошибок API
     */
    @Getter
    public enum ApiErrorType {
        // Аутентификация и авторизация
        AUTHENTICATION_FAILED(
                "Ошибка аутентификации",
                false, true, 401, 300
        ),
        INVALID_API_KEY(
                "Неверный API ключ",
                false, true, 401, 0
        ),
        INVALID_SIGNATURE(
                "Неверная подпись запроса",
                false, true, 401, 0
        ),
        API_KEY_EXPIRED(
                "API ключ истек",
                false, true, 401, 0
        ),
        INSUFFICIENT_PERMISSIONS(
                "Недостаточно прав доступа",
                false, true, 403, 0
        ),

        // Лимиты запросов
        RATE_LIMIT_EXCEEDED(
                "Превышен лимит запросов",
                true, false, 429, 60
        ),
        ORDER_RATE_LIMIT(
                "Превышен лимит ордеров",
                true, false, 429, 10
        ),
        IP_BANNED(
                "IP адрес заблокирован",
                false, true, 429, 3600
        ),

        // Сетевые ошибки
        CONNECTION_TIMEOUT(
                "Таймаут соединения",
                true, false, 408, 5
        ),
        CONNECTION_FAILED(
                "Ошибка соединения",
                true, false, 503, 30
        ),
        READ_TIMEOUT(
                "Таймаут чтения данных",
                true, false, 408, 5
        ),
        DNS_RESOLUTION_FAILED(
                "Ошибка разрешения DNS",
                true, false, 503, 60
        ),

        // Ошибки сервера биржи
        INTERNAL_SERVER_ERROR(
                "Внутренняя ошибка сервера биржи",
                true, false, 500, 30
        ),
        SERVICE_UNAVAILABLE(
                "Сервис недоступен",
                true, false, 503, 60
        ),
        MAINTENANCE_MODE(
                "Техническое обслуживание",
                true, false, 503, 300
        ),
        OVERLOADED(
                "Сервер перегружен",
                true, false, 503, 60
        ),

        // Валидация параметров
        INVALID_SYMBOL(
                "Неверная торговая пара",
                false, false, 400, 0
        ),
        INVALID_ORDER_TYPE(
                "Неверный тип ордера",
                false, false, 400, 0
        ),
        INVALID_SIDE(
                "Неверная сторона ордера",
                false, false, 400, 0
        ),
        INVALID_QUANTITY(
                "Неверное количество",
                false, false, 400, 0
        ),
        INVALID_PRICE(
                "Неверная цена",
                false, false, 400, 0
        ),
        PRECISION_OVER_MAXIMUM(
                "Превышена точность",
                false, false, 400, 0
        ),

        // Торговые ошибки
        INSUFFICIENT_BALANCE(
                "Недостаточный баланс",
                true, false, 400, 5
        ),
        MARKET_CLOSED(
                "Рынок закрыт",
                true, false, 400, 300
        ),
        ORDER_NOT_FOUND(
                "Ордер не найден",
                false, false, 404, 0
        ),
        ORDER_ALREADY_CANCELED(
                "Ордер уже отменен",
                false, false, 400, 0
        ),
        MINIMUM_NOTIONAL(
                "Не достигнут минимальный объем",
                false, false, 400, 0
        ),
        LOT_SIZE_ERROR(
                "Ошибка размера лота",
                false, false, 400, 0
        ),
        PRICE_FILTER_ERROR(
                "Ошибка ценового фильтра",
                false, false, 400, 0
        ),

        // Специфичные ошибки Binance
        BINANCE_TIMESTAMP_ERROR(
                "Ошибка временной метки Binance",
                true, false, 400, 1
        ),
        BINANCE_RECV_WINDOW_ERROR(
                "Ошибка окна получения Binance",
                true, false, 400, 1
        ),

        // Специфичные ошибки Bybit
        BYBIT_POSITION_NOT_EXISTS(
                "Позиция не существует в Bybit",
                false, false, 400, 0
        ),
        BYBIT_INSUFFICIENT_WALLET_BALANCE(
                "Недостаточный баланс кошелька Bybit",
                true, false, 400, 5
        ),

        // Прочие ошибки
        UNKNOWN_ERROR(
                "Неизвестная ошибка",
                true, false, 500, 60
        ),
        PARSING_ERROR(
                "Ошибка парсинга ответа",
                true, false, 500, 30
        ),
        UNEXPECTED_RESPONSE(
                "Неожиданный ответ сервера",
                true, false, 500, 30
        );

        private final String description;
        private final boolean retryable;
        private final boolean critical;
        private final int expectedHttpCode;
        private final int defaultRetryDelaySeconds;

        ApiErrorType(String description, boolean retryable, boolean critical,
                     int expectedHttpCode, int defaultRetryDelaySeconds) {
            this.description = description;
            this.retryable = retryable;
            this.critical = critical;
            this.expectedHttpCode = expectedHttpCode;
            this.defaultRetryDelaySeconds = defaultRetryDelaySeconds;
        }
    }

    /**
     * Конструктор с полными параметрами
     */
    public ExchangeApiException(String exchangeName, ApiErrorType errorType, String message,
                                Integer httpStatusCode, String exchangeErrorCode,
                                Long retryAfterMs, Integer remainingRequests,
                                Integer requestLimit, Long limitPeriodMs,
                                Map<String, Object> errorDetails) {
        super(formatMessage(exchangeName, errorType, message, exchangeErrorCode));
        this.exchangeName = exchangeName;
        this.errorType = errorType;
        this.httpStatusCode = httpStatusCode;
        this.exchangeErrorCode = exchangeErrorCode;
        this.retryAfterMs = retryAfterMs;
        this.remainingRequests = remainingRequests;
        this.requestLimit = requestLimit;
        this.limitPeriodMs = limitPeriodMs;
        this.errorDetails = errorDetails;
        this.timestamp = LocalDateTime.now();
        this.retryable = errorType.isRetryable();
        this.critical = errorType.isCritical();
    }

    /**
     * Упрощенный конструктор
     */
    public ExchangeApiException(String exchangeName, ApiErrorType errorType,
                                String message, Integer httpStatusCode) {
        this(exchangeName, errorType, message, httpStatusCode, null,
                null, null, null, null, null);
    }

    /**
     * Конструктор с причиной
     */
    public ExchangeApiException(String exchangeName, ApiErrorType errorType,
                                String message, Throwable cause) {
        super(formatMessage(exchangeName, errorType, message, null), cause);
        this.exchangeName = exchangeName;
        this.errorType = errorType;
        this.httpStatusCode = null;
        this.exchangeErrorCode = null;
        this.retryAfterMs = null;
        this.remainingRequests = null;
        this.requestLimit = null;
        this.limitPeriodMs = null;
        this.errorDetails = null;
        this.timestamp = LocalDateTime.now();
        this.retryable = errorType.isRetryable();
        this.critical = errorType.isCritical();
    }

    /**
     * Статические методы для создания типичных исключений
     */

    /**
     * Превышен лимит запросов
     */
    public static ExchangeApiException rateLimitExceeded(String exchangeName,
                                                         Long retryAfterMs,
                                                         Integer remainingRequests,
                                                         Integer requestLimit) {
        String message = String.format("Лимит запросов исчерпан. Осталось: %d, лимит: %d",
                remainingRequests != null ? remainingRequests : 0,
                requestLimit != null ? requestLimit : 0);
        return new ExchangeApiException(exchangeName, ApiErrorType.RATE_LIMIT_EXCEEDED,
                message, 429, null, retryAfterMs, remainingRequests,
                requestLimit, null, null);
    }

    /**
     * Ошибка аутентификации
     */
    public static ExchangeApiException authenticationFailed(String exchangeName,
                                                            String reason) {
        return new ExchangeApiException(exchangeName, ApiErrorType.AUTHENTICATION_FAILED,
                reason, 401);
    }

    /**
     * Таймаут соединения
     */
    public static ExchangeApiException connectionTimeout(String exchangeName,
                                                         int timeoutMs,
                                                         Throwable cause) {
        String message = String.format("Таймаут соединения: %d мс", timeoutMs);
        return new ExchangeApiException(exchangeName, ApiErrorType.CONNECTION_TIMEOUT,
                message, cause);
    }

    /**
     * Недостаточный баланс
     */
    public static ExchangeApiException insufficientBalance(String exchangeName,
                                                           String asset,
                                                           String required,
                                                           String available) {
        String message = String.format("Недостаточно %s. Требуется: %s, доступно: %s",
                asset, required, available);
        return new ExchangeApiException(exchangeName, ApiErrorType.INSUFFICIENT_BALANCE,
                message, 400);
    }

    /**
     * Неверная торговая пара
     */
    public static ExchangeApiException invalidSymbol(String exchangeName, String symbol) {
        String message = String.format("Торговая пара %s не поддерживается", symbol);
        return new ExchangeApiException(exchangeName, ApiErrorType.INVALID_SYMBOL,
                message, 400);
    }

    /**
     * Техническое обслуживание
     */
    public static ExchangeApiException maintenanceMode(String exchangeName,
                                                       String estimatedDuration) {
        String message = String.format("Техническое обслуживание. Ожидаемая длительность: %s",
                estimatedDuration);
        return new ExchangeApiException(exchangeName, ApiErrorType.MAINTENANCE_MODE,
                message, 503);
    }

    /**
     * Ошибка временной метки Binance
     */
    public static ExchangeApiException binanceTimestampError(long serverTime, long clientTime) {
        String message = String.format("Временная метка за пределами recv_window. " +
                        "Сервер: %d, клиент: %d, разница: %d мс",
                serverTime, clientTime, Math.abs(serverTime - clientTime));
        return new ExchangeApiException("Binance", ApiErrorType.BINANCE_TIMESTAMP_ERROR,
                message, 400);
    }

    /**
     * Форматирование сообщения об ошибке
     */
    private static String formatMessage(String exchangeName, ApiErrorType errorType,
                                        String message, String exchangeErrorCode) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(exchangeName != null ? exchangeName.toUpperCase() : "EXCHANGE");
        sb.append("-API] ").append(errorType.getDescription());

        if (exchangeErrorCode != null && !exchangeErrorCode.isEmpty()) {
            sb.append(" (код: ").append(exchangeErrorCode).append(")");
        }

        if (message != null && !message.isEmpty()) {
            sb.append(": ").append(message);
        }

        return sb.toString();
    }

    /**
     * Получить рекомендуемую задержку перед повторной попыткой
     */
    public long getRetryDelayMs() {
        if (!retryable) {
            return -1; // повтор невозможен
        }

        // Если биржа указала точное время
        if (retryAfterMs != null && retryAfterMs > 0) {
            return retryAfterMs;
        }

        // Используем значение по умолчанию
        return errorType.getDefaultRetryDelaySeconds() * 1000L;
    }

    /**
     * Получить рекомендуемое действие
     */
    public String getRecommendedAction() {
        switch (errorType) {
            case AUTHENTICATION_FAILED:
            case INVALID_API_KEY:
            case INVALID_SIGNATURE:
                return "Проверить API ключи и подпись запросов";

            case API_KEY_EXPIRED:
                return "Обновить API ключи";

            case INSUFFICIENT_PERMISSIONS:
                return "Проверить права доступа API ключей";

            case RATE_LIMIT_EXCEEDED:
            case ORDER_RATE_LIMIT:
                return String.format("Подождать %d секунд перед следующим запросом",
                        getRetryDelayMs() / 1000);

            case IP_BANNED:
                return "Обратиться в поддержку биржи для разблокировки IP";

            case CONNECTION_TIMEOUT:
            case CONNECTION_FAILED:
                return "Проверить сетевое соединение и повторить";

            case INTERNAL_SERVER_ERROR:
            case SERVICE_UNAVAILABLE:
                return "Подождать восстановления сервиса биржи";

            case MAINTENANCE_MODE:
                return "Дождаться завершения технического обслуживания";

            case INSUFFICIENT_BALANCE:
                return "Пополнить баланс или уменьшить размер ордера";

            case INVALID_SYMBOL:
                return "Проверить корректность торговой пары";

            case BINANCE_TIMESTAMP_ERROR:
                return "Синхронизировать системное время";

            default:
                return "Проанализировать детали ошибки и принять соответствующие меры";
        }
    }

    /**
     * Нужно ли уведомление для данной ошибки
     */
    public boolean shouldNotify() {
        return critical ||
                errorType == ApiErrorType.RATE_LIMIT_EXCEEDED ||
                errorType == ApiErrorType.AUTHENTICATION_FAILED ||
                errorType == ApiErrorType.INSUFFICIENT_BALANCE ||
                errorType == ApiErrorType.MAINTENANCE_MODE;
    }

    /**
     * Получить приоритет уведомления (1-5)
     */
    public int getNotificationPriority() {
        if (critical) {
            return 5; // критический
        }

        switch (errorType) {
            case RATE_LIMIT_EXCEEDED:
            case ORDER_RATE_LIMIT:
                return 4; // высокий
            case INSUFFICIENT_BALANCE:
            case MAINTENANCE_MODE:
                return 3; // средний
            case CONNECTION_TIMEOUT:
            case CONNECTION_FAILED:
                return 2; // низкий
            default:
                return 1; // информационный
        }
    }

    /**
     * Проверить, связана ли ошибка с сетью
     */
    public boolean isNetworkError() {
        return errorType == ApiErrorType.CONNECTION_TIMEOUT ||
                errorType == ApiErrorType.CONNECTION_FAILED ||
                errorType == ApiErrorType.READ_TIMEOUT ||
                errorType == ApiErrorType.DNS_RESOLUTION_FAILED;
    }

    /**
     * Проверить, связана ли ошибка с лимитами
     */
    public boolean isRateLimitError() {
        return errorType == ApiErrorType.RATE_LIMIT_EXCEEDED ||
                errorType == ApiErrorType.ORDER_RATE_LIMIT ||
                errorType == ApiErrorType.IP_BANNED;
    }

    /**
     * Проверить, связана ли ошибка с аутентификацией
     */
    public boolean isAuthenticationError() {
        return errorType == ApiErrorType.AUTHENTICATION_FAILED ||
                errorType == ApiErrorType.INVALID_API_KEY ||
                errorType == ApiErrorType.INVALID_SIGNATURE ||
                errorType == ApiErrorType.API_KEY_EXPIRED ||
                errorType == ApiErrorType.INSUFFICIENT_PERMISSIONS;
    }

    /**
     * Создать детальную информацию об ошибке
     */
    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Ошибка API биржи:\n");
        sb.append("Биржа: ").append(exchangeName).append("\n");
        sb.append("Тип: ").append(errorType.getDescription()).append("\n");
        sb.append("Время: ").append(timestamp).append("\n");
        sb.append("Критическая: ").append(critical ? "ДА" : "НЕТ").append("\n");
        sb.append("Можно повторить: ").append(retryable ? "ДА" : "НЕТ").append("\n");

        if (httpStatusCode != null) {
            sb.append("HTTP статус: ").append(httpStatusCode).append("\n");
        }
        if (exchangeErrorCode != null) {
            sb.append("Код ошибки биржи: ").append(exchangeErrorCode).append("\n");
        }
        if (retryAfterMs != null) {
            sb.append("Повторить через: ").append(retryAfterMs / 1000).append(" сек\n");
        }
        if (remainingRequests != null) {
            sb.append("Осталось запросов: ").append(remainingRequests).append("\n");
        }
        if (requestLimit != null) {
            sb.append("Лимит запросов: ").append(requestLimit).append("\n");
        }
        if (errorDetails != null && !errorDetails.isEmpty()) {
            sb.append("Дополнительные детали: ").append(errorDetails).append("\n");
        }

        sb.append("Рекомендуемое действие: ").append(getRecommendedAction());

        return sb.toString();
    }
}