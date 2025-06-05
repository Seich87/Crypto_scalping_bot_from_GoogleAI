package com.example.scalpingBot.exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Глобальный обработчик исключений для скальпинг-бота
 *
 * Обрабатывает все типы исключений:
 * - Торговые исключения (TradingException)
 * - Исключения управления рисками (RiskManagementException)
 * - Исключения API бирж (ExchangeApiException)
 * - Валидационные исключения
 * - Системные исключения
 *
 * Формирует единообразные HTTP ответы с детальной информацией
 * для фронтенда и системы мониторинга.
 *
 * @author ScalpingBot Team
 * @version 1.0
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    /**
     * Структура стандартного ответа об ошибке
     */
    public static class ErrorResponse {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private String path;
        private String errorCode;
        private String errorType;
        private boolean retryable;
        private boolean critical;
        private Long retryAfterMs;
        private String recommendedAction;
        private Map<String, Object> details;

        public ErrorResponse() {
            this.timestamp = LocalDateTime.now();
            this.details = new HashMap<>();
        }

        // Getters and Setters
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

        public String getErrorType() { return errorType; }
        public void setErrorType(String errorType) { this.errorType = errorType; }

        public boolean isRetryable() { return retryable; }
        public void setRetryable(boolean retryable) { this.retryable = retryable; }

        public boolean isCritical() { return critical; }
        public void setCritical(boolean critical) { this.critical = critical; }

        public Long getRetryAfterMs() { return retryAfterMs; }
        public void setRetryAfterMs(Long retryAfterMs) { this.retryAfterMs = retryAfterMs; }

        public String getRecommendedAction() { return recommendedAction; }
        public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }

        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { this.details = details; }
    }

    /**
     * Обработка торговых исключений
     */
    @ExceptionHandler(TradingException.class)
    public ResponseEntity<ErrorResponse> handleTradingException(
            TradingException ex, WebRequest request) {

        log.error("Trading exception occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(determineHttpStatus(ex).value());
        errorResponse.setError("Trading Error");
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setPath(request.getDescription(false));
        errorResponse.setErrorCode(ex.getErrorCode());
        errorResponse.setErrorType("TRADING_EXCEPTION");
        errorResponse.setRetryable(ex.isRetryable());
        errorResponse.setCritical(ex.isCritical());
        errorResponse.setRecommendedAction(ex.getRecommendedAction());

        if (ex.isRetryable()) {
            errorResponse.setRetryAfterMs((long) ex.getRetryDelaySeconds() * 1000);
        }

        // Добавляем детали торговой ошибки
        Map<String, Object> details = errorResponse.getDetails();
        if (ex.getTradingPair() != null) {
            details.put("tradingPair", ex.getTradingPair());
        }
        if (ex.getPositionSize() != null) {
            details.put("positionSize", ex.getPositionSize());
        }
        if (ex.getPrice() != null) {
            details.put("price", ex.getPrice());
        }
        if (ex.getOrderStatus() != null) {
            details.put("orderStatus", ex.getOrderStatus());
        }
        if (ex.getRiskLevel() != null) {
            details.put("riskLevel", ex.getRiskLevel());
        }
        details.put("notificationPriority", ex.getNotificationPriority());

        // Отправляем уведомление для критических ошибок
        if (ex.shouldNotify()) {
            sendNotification(ex);
        }

        return new ResponseEntity<>(errorResponse, determineHttpStatus(ex));
    }

    /**
     * Обработка исключений управления рисками
     */
    @ExceptionHandler(RiskManagementException.class)
    public ResponseEntity<ErrorResponse> handleRiskManagementException(
            RiskManagementException ex, WebRequest request) {

        log.error("Risk management exception occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(HttpStatus.FORBIDDEN.value());
        errorResponse.setError("Risk Management Error");
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setPath(request.getDescription(false));
        errorResponse.setErrorCode(ex.getEventCode());
        errorResponse.setErrorType("RISK_MANAGEMENT_EXCEPTION");
        errorResponse.setRetryable(!ex.isRequiresImmediateAction());
        errorResponse.setCritical(ex.getSeverityLevel() >= 4);
        errorResponse.setRecommendedAction(ex.getRecommendedActions());

        if (ex.isAutoClosePositions()) {
            errorResponse.setRetryAfterMs((long) ex.getAutoCloseTimeoutSeconds() * 1000);
        }

        // Добавляем детали риск-события
        Map<String, Object> details = errorResponse.getDetails();
        details.put("severityLevel", ex.getSeverityLevel());
        details.put("requiresImmediateAction", ex.isRequiresImmediateAction());
        details.put("autoClosePositions", ex.isAutoClosePositions());

        if (ex.getCurrentRiskLevel() != null) {
            details.put("currentRiskLevel", ex.getCurrentRiskLevel());
        }
        if (ex.getPreviousRiskLevel() != null) {
            details.put("previousRiskLevel", ex.getPreviousRiskLevel());
        }
        if (ex.getCurrentValue() != null && ex.getThresholdValue() != null) {
            details.put("currentValue", ex.getCurrentValue());
            details.put("thresholdValue", ex.getThresholdValue());
            details.put("excess", ex.getCurrentValue().subtract(ex.getThresholdValue()));
        }
        if (ex.getAffectedPairs() != null) {
            details.put("affectedPairs", ex.getAffectedPairs());
        }
        if (ex.getAffectedPositions() != null) {
            details.put("affectedPositions", ex.getAffectedPositions());
        }
        if (ex.getPortfolioExposure() != null) {
            details.put("portfolioExposure", ex.getPortfolioExposure());
        }

        // Критические риск-события требуют немедленного уведомления
        if (ex.requiresImmediateNotification()) {
            sendCriticalRiskNotification(ex);
        }

        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    /**
     * Обработка исключений API бирж
     */
    @ExceptionHandler(ExchangeApiException.class)
    public ResponseEntity<ErrorResponse> handleExchangeApiException(
            ExchangeApiException ex, WebRequest request) {

        log.error("Exchange API exception occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(ex.getHttpStatusCode() != null ?
                ex.getHttpStatusCode() : HttpStatus.BAD_GATEWAY.value());
        errorResponse.setError("Exchange API Error");
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setPath(request.getDescription(false));
        errorResponse.setErrorCode(ex.getErrorType().name());
        errorResponse.setErrorType("EXCHANGE_API_EXCEPTION");
        errorResponse.setRetryable(ex.isRetryable());
        errorResponse.setCritical(ex.isCritical());
        errorResponse.setRecommendedAction(ex.getRecommendedAction());

        if (ex.isRetryable()) {
            errorResponse.setRetryAfterMs(ex.getRetryDelayMs());
        }

        // Добавляем детали API ошибки
        Map<String, Object> details = errorResponse.getDetails();
        details.put("exchangeName", ex.getExchangeName());
        details.put("isNetworkError", ex.isNetworkError());
        details.put("isRateLimitError", ex.isRateLimitError());
        details.put("isAuthenticationError", ex.isAuthenticationError());

        if (ex.getExchangeErrorCode() != null) {
            details.put("exchangeErrorCode", ex.getExchangeErrorCode());
        }
        if (ex.getRemainingRequests() != null) {
            details.put("remainingRequests", ex.getRemainingRequests());
        }
        if (ex.getRequestLimit() != null) {
            details.put("requestLimit", ex.getRequestLimit());
        }
        if (ex.getErrorDetails() != null) {
            details.put("exchangeDetails", ex.getErrorDetails());
        }
        details.put("notificationPriority", ex.getNotificationPriority());

        // Уведомляем о критических API ошибках
        if (ex.shouldNotify()) {
            sendExchangeApiNotification(ex);
        }

        HttpStatus httpStatus = ex.getHttpStatusCode() != null ?
                HttpStatus.valueOf(ex.getHttpStatusCode()) : HttpStatus.BAD_GATEWAY;

        return new ResponseEntity<>(errorResponse, httpStatus);
    }

    /**
     * Обработка валидационных исключений Bean Validation
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {

        log.warn("Validation exception occurred: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        errorResponse.setError("Validation Error");
        errorResponse.setPath(request.getDescription(false));
        errorResponse.setErrorCode("VALIDATION_FAILED");
        errorResponse.setErrorType("VALIDATION_EXCEPTION");
        errorResponse.setRetryable(true);
        errorResponse.setCritical(false);
        errorResponse.setRecommendedAction("Исправить параметры запроса согласно требованиям валидации");

        // Собираем все ошибки валидации
        List<String> validationErrors = new ArrayList<>();
        Map<String, String> fieldErrors = new HashMap<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            String error = String.format("Поле '%s': %s",
                    fieldError.getField(), fieldError.getDefaultMessage());
            validationErrors.add(error);
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        errorResponse.setMessage(String.format("Ошибки валидации: %s",
                String.join("; ", validationErrors)));

        Map<String, Object> details = errorResponse.getDetails();
        details.put("validationErrors", validationErrors);
        details.put("fieldErrors", fieldErrors);
        details.put("errorCount", validationErrors.size());

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Обработка исключений валидации ConstraintViolation
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {

        log.warn("Constraint violation exception occurred: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        errorResponse.setError("Constraint Violation Error");
        errorResponse.setPath(request.getDescription(false));
        errorResponse.setErrorCode("CONSTRAINT_VIOLATION");
        errorResponse.setErrorType("CONSTRAINT_VIOLATION_EXCEPTION");
        errorResponse.setRetryable(true);
        errorResponse.setCritical(false);
        errorResponse.setRecommendedAction("Исправить параметры согласно ограничениям");

        List<String> violations = ex.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        errorResponse.setMessage(String.format("Нарушения ограничений: %s",
                String.join("; ", violations)));

        Map<String, Object> details = errorResponse.getDetails();
        details.put("constraintViolations", violations);
        details.put("violationCount", violations.size());

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Обработка исключений неверного типа аргументов
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex, WebRequest request) {

        log.warn("Type mismatch exception occurred: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        errorResponse.setError("Type Mismatch Error");
        errorResponse.setMessage(String.format("Неверный тип параметра '%s'. Ожидается: %s",
                ex.getName(),
                ex.getRequiredType() != null ?
                        ex.getRequiredType().getSimpleName() : "неизвестен"));
        errorResponse.setPath(request.getDescription(false));
        errorResponse.setErrorCode("TYPE_MISMATCH");
        errorResponse.setErrorType("TYPE_MISMATCH_EXCEPTION");
        errorResponse.setRetryable(true);
        errorResponse.setCritical(false);
        errorResponse.setRecommendedAction("Проверить типы параметров запроса");

        Map<String, Object> details = errorResponse.getDetails();
        details.put("parameterName", ex.getName());
        details.put("providedValue", ex.getValue());
        details.put("requiredType", ex.getRequiredType() != null ?
                ex.getRequiredType().getSimpleName() : "неизвестен");

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Обработка всех остальных исключений
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {

        log.error("Unexpected exception occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.setError("Internal Server Error");
        errorResponse.setMessage("Произошла внутренняя ошибка сервера");
        errorResponse.setPath(request.getDescription(false));
        errorResponse.setErrorCode("INTERNAL_ERROR");
        errorResponse.setErrorType("GENERIC_EXCEPTION");
        errorResponse.setRetryable(true);
        errorResponse.setCritical(true);
        errorResponse.setRecommendedAction("Обратиться к администратору системы");
        errorResponse.setRetryAfterMs(30000L); // 30 секунд

        Map<String, Object> details = errorResponse.getDetails();
        details.put("exceptionType", ex.getClass().getSimpleName());
        details.put("stackTrace", getStackTraceAsString(ex));

        // Критические системные ошибки требуют немедленного уведомления
        sendCriticalSystemNotification(ex, request);

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Определить HTTP статус для торговых исключений
     */
    private HttpStatus determineHttpStatus(TradingException ex) {
        switch (ex.getErrorType()) {
            case INSUFFICIENT_BALANCE:
            case INVALID_POSITION_SIZE:
            case INVALID_PRICE:
            case INVALID_TRADING_PAIR:
                return HttpStatus.BAD_REQUEST;

            case POSITION_LIMIT_EXCEEDED:
            case DAILY_LOSS_LIMIT_EXCEEDED:
            case RISK_LIMIT_EXCEEDED:
                return HttpStatus.FORBIDDEN;

            case MARKET_CLOSED:
            case LOW_LIQUIDITY:
                return HttpStatus.SERVICE_UNAVAILABLE;

            case EXCHANGE_CONNECTION_ERROR:
            case API_ERROR:
                return HttpStatus.BAD_GATEWAY;

            case EMERGENCY_STOP_TRIGGERED:
            case SYSTEM_ERROR:
                return HttpStatus.INTERNAL_SERVER_ERROR;

            default:
                return HttpStatus.BAD_REQUEST;
        }
    }

    /**
     * Отправка уведомления о торговой ошибке
     */
    private void sendNotification(TradingException ex) {
        // Здесь будет интеграция с сервисом уведомлений
        log.info("Sending trading exception notification: {}", ex.getErrorCode());
        // notificationService.sendTradingAlert(ex);
    }

    /**
     * Отправка критического уведомления о риске
     */
    private void sendCriticalRiskNotification(RiskManagementException ex) {
        // Здесь будет интеграция с сервисом критических уведомлений
        log.error("Sending CRITICAL risk notification: {}", ex.getEventCode());
        // notificationService.sendCriticalRiskAlert(ex);
    }

    /**
     * Отправка уведомления об ошибке API биржи
     */
    private void sendExchangeApiNotification(ExchangeApiException ex) {
        // Здесь будет интеграция с сервисом уведомлений
        log.warn("Sending exchange API notification: {} - {}",
                ex.getExchangeName(), ex.getErrorType());
        // notificationService.sendExchangeAlert(ex);
    }

    /**
     * Отправка критического системного уведомления
     */
    private void sendCriticalSystemNotification(Exception ex, WebRequest request) {
        // Здесь будет интеграция с сервисом критических уведомлений
        log.error("Sending CRITICAL system notification: {}", ex.getClass().getSimpleName());
        // notificationService.sendCriticalSystemAlert(ex, request);
    }

    /**
     * Получить stack trace как строку
     */
    private String getStackTraceAsString(Exception ex) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        ex.printStackTrace(pw);
        return sw.toString();
    }
}