package com.example.scalpingBot.service.websocket;

import com.example.scalpingBot.service.event.MarketDataEventPublisher;
import com.example.scalpingBot.service.market.MarketDataService;
import jakarta.websocket.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
public class BinanceWebSocketHandler extends Endpoint {

    private static final Logger log = LoggerFactory.getLogger(BinanceWebSocketHandler.class);

    // Статические переменные для доступа из методов Endpoint
    private static WebSocketClientService clientService;
    private static MarketDataEventPublisher eventPublisher;
    private static MarketDataService marketDataService;
    private static List<String> tradingPairs;
    private static String wsBaseUrl;

    // Управление состоянием соединения
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicBoolean isReconnecting = new AtomicBoolean(false);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // Конфигурация реконнекта
    private static final int MAX_RECONNECT_ATTEMPTS = 10;
    private static final int INITIAL_RECONNECT_DELAY_SECONDS = 5;
    private static final int MAX_RECONNECT_DELAY_SECONDS = 300; // 5 минут
    private static final int HEARTBEAT_INTERVAL_SECONDS = 30;
    private static final int CONNECTION_TIMEOUT_SECONDS = 60;

    // Fallback механизм
    private final AtomicBoolean fallbackMode = new AtomicBoolean(false);
    private volatile Session currentSession;

    @Autowired
    public void setDependencies(WebSocketClientService clientService,
                                MarketDataEventPublisher eventPublisher,
                                MarketDataService marketDataService,
                                @Value("${bot.trading-pairs}") List<String> tradingPairs,
                                @Value("${binance.ws.base-url}") String wsBaseUrl) {
        BinanceWebSocketHandler.clientService = clientService;
        BinanceWebSocketHandler.eventPublisher = eventPublisher;
        BinanceWebSocketHandler.marketDataService = marketDataService;
        BinanceWebSocketHandler.tradingPairs = tradingPairs;
        BinanceWebSocketHandler.wsBaseUrl = wsBaseUrl;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        if (tradingPairs == null || tradingPairs.isEmpty()) {
            log.warn("No trading pairs configured for WebSocket.");
            return;
        }

        log.info("Initializing Binance WebSocket for {} pairs: {}", tradingPairs.size(), tradingPairs);
        connectWithRetry();
        startHeartbeatMonitor();
        startFallbackScheduler();
    }

    /**
     * Подключение с автоматическими повторными попытками
     */
    private void connectWithRetry() {
        if (isReconnecting.get()) {
            return; // Уже идет процесс переподключения
        }

        String streams = tradingPairs.stream()
                .map(pair -> pair.toLowerCase() + "@trade/" + pair.toLowerCase() + "@miniTicker")
                .collect(Collectors.joining("/"));
        String url = wsBaseUrl + "/stream?streams=" + streams;

        log.info("Attempting to connect to WebSocket: {}", url);

        try {
            clientService.connect(url, this);
        } catch (Exception e) {
            log.error("Failed to initiate WebSocket connection: {}", e.getMessage());
            scheduleReconnect();
        }
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        this.currentSession = session;
        isConnected.set(true);
        isReconnecting.set(false);
        reconnectAttempts.set(0);
        fallbackMode.set(false);

        log.info("✅ WebSocket connection established successfully: {}", session.getId());

        // Регистрируем обработчик сообщений
        session.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
                try {
                    log.trace("📨 Received WebSocket message: {}", message);
                    eventPublisher.publishTickerEvent(message);
                } catch (Exception e) {
                    log.error("Error processing WebSocket message: {}", e.getMessage(), e);
                }
            }
        });

        // Настраиваем таймаут для сессии
        session.setMaxIdleTimeout(CONNECTION_TIMEOUT_SECONDS * 1000L);
    }

    @Override
    public void onError(Session session, Throwable error) {
        log.error("❌ WebSocket error on session {}: {}",
                session != null ? session.getId() : "unknown", error.getMessage(), error);

        isConnected.set(false);
        scheduleReconnect();
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        isConnected.set(false);

        log.warn("🔌 WebSocket connection closed: {}. Reason: {} (Code: {})",
                session.getId(), closeReason.getReasonPhrase(), closeReason.getCloseCode());

        // Проверяем, было ли это ожидаемое закрытие
        if (closeReason.getCloseCode() != CloseReason.CloseCodes.NORMAL_CLOSURE &&
                closeReason.getCloseCode() != CloseReason.CloseCodes.GOING_AWAY) {
            scheduleReconnect();
        }
    }

    /**
     * Планирует переподключение с экспоненциальной задержкой
     */
    private void scheduleReconnect() {
        if (isReconnecting.compareAndSet(false, true)) {
            int attempt = reconnectAttempts.incrementAndGet();

            if (attempt > MAX_RECONNECT_ATTEMPTS) {
                log.error("🚨 Maximum reconnection attempts ({}) exceeded. Switching to fallback mode.",
                        MAX_RECONNECT_ATTEMPTS);
                enableFallbackMode();
                return;
            }

            // Экспоненциальная задержка: min(INITIAL_DELAY * 2^attempt, MAX_DELAY)
            int delay = Math.min(
                    INITIAL_RECONNECT_DELAY_SECONDS * (int) Math.pow(2, attempt - 1),
                    MAX_RECONNECT_DELAY_SECONDS
            );

            log.warn("🔄 Scheduling reconnection attempt {} in {} seconds...", attempt, delay);

            scheduler.schedule(() -> {
                try {
                    connectWithRetry();
                } catch (Exception e) {
                    log.error("Error during scheduled reconnection: {}", e.getMessage());
                    isReconnecting.set(false);
                    scheduleReconnect(); // Попробуем еще раз
                }
            }, delay, TimeUnit.SECONDS);
        }
    }

    /**
     * Включает режим fallback - получение данных через REST API
     */
    private void enableFallbackMode() {
        fallbackMode.set(true);
        isReconnecting.set(false);

        log.warn("🔄 Enabled fallback mode - using REST API for market data");

        // Запускаем периодическое получение данных через REST
        scheduler.scheduleAtFixedRate(this::fetchDataViaRest, 0, 5, TimeUnit.SECONDS);

        // Периодически пытаемся восстановить WebSocket соединение
        scheduler.scheduleAtFixedRate(() -> {
            if (fallbackMode.get() && !isConnected.get()) {
                log.info("🔄 Attempting to restore WebSocket connection from fallback mode...");
                reconnectAttempts.set(0); // Сбрасываем счетчик попыток
                connectWithRetry();
            }
        }, 60, 60, TimeUnit.SECONDS); // Каждую минуту
    }

    /**
     * Получение данных через REST API как fallback
     */
    private void fetchDataViaRest() {
        if (!fallbackMode.get()) {
            return; // WebSocket восстановлен
        }

        for (String pair : tradingPairs) {
            try {
                var ticker = marketDataService.getCurrentTicker(pair);
                eventPublisher.publishTickerEvent(
                        String.format("""
                        {
                          "stream": "%s@ticker",
                          "data": {
                            "e": "24hrTicker",
                            "s": "%s",
                            "c": "%s",
                            "b": "%s",
                            "a": "%s",
                            "v": "%s",
                            "q": "%s",
                            "P": "%s",
                            "E": %d
                          }
                        }
                        """,
                                pair.toLowerCase(),
                                ticker.getSymbol(),
                                ticker.getLastPrice(),
                                ticker.getBestBidPrice() != null ? ticker.getBestBidPrice() : "0",
                                ticker.getBestAskPrice() != null ? ticker.getBestAskPrice() : "0",
                                ticker.getVolume24h() != null ? ticker.getVolume24h() : "0",
                                ticker.getQuoteVolume24h() != null ? ticker.getQuoteVolume24h() : "0",
                                ticker.getPriceChangePercent24h() != null ? ticker.getPriceChangePercent24h() : "0",
                                System.currentTimeMillis()
                        )
                );
                log.trace("📡 Fetched fallback data for {}", pair);
            } catch (Exception e) {
                log.error("Failed to fetch fallback data for {}: {}", pair, e.getMessage());
            }
        }
    }

    /**
     * Мониторинг соединения через heartbeat
     */
    private void startHeartbeatMonitor() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (isConnected.get() && currentSession != null && currentSession.isOpen()) {
                    // Отправляем ping для проверки соединения
                    currentSession.getAsyncRemote().sendPing(java.nio.ByteBuffer.wrap("ping".getBytes()));
                    log.trace("💓 Sent heartbeat ping");
                } else if (isConnected.get()) {
                    log.warn("💔 Heartbeat detected disconnected session");
                    isConnected.set(false);
                    scheduleReconnect();
                }
            } catch (Exception e) {
                log.error("Error during heartbeat check: {}", e.getMessage());
                isConnected.set(false);
                scheduleReconnect();
            }
        }, HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Планировщик для мониторинга fallback режима
     */
    private void startFallbackScheduler() {
        scheduler.scheduleAtFixedRate(() -> {
            if (fallbackMode.get()) {
                log.info("📊 Currently in fallback mode - using REST API for market data");
            } else if (isConnected.get()) {
                log.debug("📡 WebSocket connection healthy");
            }
        }, 60, 60, TimeUnit.SECONDS);
    }

    /**
     * Получение текущего статуса соединения
     */
    public boolean isConnected() {
        return isConnected.get();
    }

    /**
     * Проверка, активен ли fallback режим
     */
    public boolean isFallbackMode() {
        return fallbackMode.get();
    }

    /**
     * Принудительное переподключение
     */
    public void forceReconnect() {
        log.info("🔄 Force reconnection requested");
        if (currentSession != null && currentSession.isOpen()) {
            try {
                currentSession.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Force reconnect"));
            } catch (Exception e) {
                log.warn("Error closing current session: {}", e.getMessage());
            }
        }
        isConnected.set(false);
        reconnectAttempts.set(0);
        scheduleReconnect();
    }

    /**
     * Graceful shutdown
     */
    public void shutdown() {
        log.info("🛑 Shutting down WebSocket handler...");

        if (currentSession != null && currentSession.isOpen()) {
            try {
                currentSession.close(new CloseReason(CloseReason.CloseCodes.GOING_AWAY, "Application shutdown"));
            } catch (Exception e) {
                log.warn("Error during graceful session close: {}", e.getMessage());
            }
        }

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("✅ WebSocket handler shutdown completed");
    }
}