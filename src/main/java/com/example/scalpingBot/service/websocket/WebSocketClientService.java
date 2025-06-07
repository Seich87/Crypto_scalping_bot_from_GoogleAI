package com.example.scalpingBot.service.websocket;

import jakarta.websocket.*;
import org.glassfish.tyrus.client.ClientManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class WebSocketClientService {
    private static final Logger log = LoggerFactory.getLogger(WebSocketClientService.class);

    public void connect(String url, Endpoint endpoint) {
        ClientManager client = ClientManager.createClient();
        try {
            log.info("Connecting to WebSocket: {}", url);
            Session session = client.connectToServer(endpoint, URI.create(url));

            // Добавляем логику переподключения, если сессия закрылась
            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
                if (!session.isOpen()) {
                    log.warn("WebSocket session closed. Attempting to reconnect...");
                    try {
                        client.connectToServer(endpoint, URI.create(url));
                        log.info("Successfully reconnected to WebSocket.");
                    } catch (DeploymentException | IOException e) {
                        log.error("Failed to reconnect to WebSocket: {}", e.getMessage());
                    }
                }
            }, 60, 60, TimeUnit.SECONDS);

        } catch (DeploymentException | IOException e) {
            log.error("Failed to connect to WebSocket [{}]: {}", url, e.getMessage());
            // Здесь можно добавить логику повторных попыток подключения с задержкой
        }
    }
}