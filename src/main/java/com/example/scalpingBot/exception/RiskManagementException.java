package com.example.scalpingBot.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Исключение для всех ошибок, связанных с логикой управления рисками.
 * Например, превышение максимального размера позиции,
 * некорректные параметры для расчета стоп-лосса.
 *
 * Аннотация @ResponseStatus(HttpStatus.BAD_REQUEST) указывает Spring,
 * что если это исключение "долетит" до контроллера,
 * нужно вернуть клиенту HTTP статус 400 Bad Request.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class RiskManagementException extends RuntimeException {

    public RiskManagementException(String message) {
        super(message);
    }

    public RiskManagementException(String message, Throwable cause) {
        super(message, cause);
    }
}