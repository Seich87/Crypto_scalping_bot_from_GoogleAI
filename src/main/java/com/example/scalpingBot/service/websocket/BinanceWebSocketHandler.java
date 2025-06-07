package com.example.scalpingBot.service.websocket;

import com.example.scalpingBot.service.event.MarketDataEventPublisher;
import jakarta.websocket.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class BinanceWebSocketHandler extends Endpoint {

    private static final Logger log = LoggerFactory.getLogger(BinanceWebSocketHandler.class);
    private static WebSocketClientService clientService;
    private static MarketDataEventPublisher eventPublisher;
    private static List<String> tradingPairs;
    private static String wsBaseUrl;

    // Статический "setter", чтобы Spring мог внедрить бины
    @Autowired
    public void setDependencies(WebSocketClientService clientService,
                                MarketDataEventPublisher eventPublisher,
                                @Value("${bot.trading-pairs}") List<String> tradingPairs,
                                @Value("${binance.ws.base-url}") String wsBaseUrl) {
        BinanceWebSocketHandler.clientService = clientService;
        BinanceWebSocketHandler.eventPublisher = eventPublisher;
        BinanceWebSocketHandler.tradingPairs = tradingPairs;
        BinanceWebSocketHandler.wsBaseUrl = wsBaseUrl;
    }

    /**
     * Запускает подключение после того, как все зависимости внедрены.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        if (tradingPairs == null || tradingPairs.isEmpty()) {
            log.warn("No trading pairs configured for WebSocket.");
            return;
        }
        String streams = tradingPairs.stream()
                .map(pair -> pair.toLowerCase() + "@trade")
                .collect(Collectors.joining("/"));
        String url = wsBaseUrl + "/stream?streams=" + streams;
        clientService.connect(url, this);
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        log.info("WebSocket connection opened: {}", session.getId());
        // РЕГИСТРИРУЕМ ОБРАБОТЧИК СООБЩЕНИЙ. Это правильный подход.
        session.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
                // Этот метод будет вызываться для каждого входящего сообщения
                log.trace("Received WebSocket message: {}", message);
                eventPublisher.publishTickerEvent(message);
            }
        });
    }

    @Override
    public void onError(Session session, Throwable thr) {
        log.error("WebSocket error on session {}: {}", session.getId(), thr.getMessage(), thr);
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        log.warn("WebSocket connection closed: {}. Reason: {}", session.getId(), closeReason.getReasonPhrase());
    }
}