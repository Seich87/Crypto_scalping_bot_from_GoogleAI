package com.example.scalpingBot.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Исключение для всех ошибок, возникающих при взаимодействии с API биржи.
 * Это могут быть ошибки сети, ошибки аутентификации, некорректные ответы от биржи,
 * превышение лимитов на количество запросов (rate limits) и т.д.
 *
 * Аннотация @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) указывает Spring,
 * что если это исключение долетит до контроллера, нужно вернуть клиенту
 * HTTP статус 500 Internal Server Error. Это сигнализирует о том, что проблема
 * произошла на стороне сервера или во внешней системе, а не в запросе клиента.
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ExchangeApiException extends RuntimeException {

    public ExchangeApiException(String message) {
        super(message);
    }

    public ExchangeApiException(String message, Throwable cause) {
        super(message, cause);
    }
}