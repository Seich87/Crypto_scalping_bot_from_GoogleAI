package com.example.scalpingBot.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Глобальный обработчик исключений для всего приложения.
 * Перехватывает исключения, выброшенные контроллерами или сервисами,
 * и формирует стандартизированный JSON-ответ об ошибке.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Обрабатывает наши кастомные бизнес-исключения TradingException и RiskManagementException.
     * Возвращает HTTP 400 Bad Request.
     */
    @ExceptionHandler({TradingException.class, RiskManagementException.class})
    public ResponseEntity<Object> handleBusinessException(RuntimeException ex, WebRequest request) {
        log.warn("Business logic error: {}", ex.getMessage());
        return createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /**
     * Обрабатывает ошибки, связанные с взаимодействием с API биржи.
     * Возвращает HTTP 500 Internal Server Error.
     */
    @ExceptionHandler(ExchangeApiException.class)
    public ResponseEntity<Object> handleExchangeApiException(ExchangeApiException ex, WebRequest request) {
        log.error("Exchange API error: {}", ex.getMessage(), ex);
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request);
    }

    /**
     * Обрабатывает ошибки валидации DTO (@Valid).
     * Возвращает HTTP 400 Bad Request со списком полей, не прошедших валидацию.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        log.warn("Validation error: {}", ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        body.put("errors", errors);

        return new ResponseEntity<>(body, headers, status);
    }

    /**
     * Перехватывает все остальные необработанные исключения.
     * Возвращает HTTP 500 Internal Server Error, чтобы не раскрывать детали системы.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllOtherExceptions(Exception ex, WebRequest request) {
        log.error("An unexpected error occurred", ex);
        String message = "An unexpected internal server error occurred.";
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, message, request);
    }

    private ResponseEntity<Object> createErrorResponse(HttpStatus status, String message, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        // Добавляем путь, на котором произошла ошибка, для удобства отладки
        // body.put("path", ((org.springframework.web.context.request.ServletWebRequest) request).getRequest().getRequestURI());

        return new ResponseEntity<>(body, status);
    }
}