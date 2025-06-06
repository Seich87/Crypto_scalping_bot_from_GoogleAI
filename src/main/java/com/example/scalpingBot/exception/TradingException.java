package com.example.scalpingBot.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Базовое исключение для всех ошибок, связанных с торговой логикой.
 * Например, попытка открыть позицию, когда она уже открыта,
 * или невозможность рассчитать размер позиции.
 *
 * Аннотация @ResponseStatus(HttpStatus.BAD_REQUEST) указывает Spring,
 * что если это исключение "долетит" до контроллера,
 * нужно вернуть клиенту HTTP статус 400 Bad Request.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class TradingException extends RuntimeException {

    public TradingException(String message) {
        super(message);
    }

    public TradingException(String message, Throwable cause) {
        super(message, cause);
    }
}