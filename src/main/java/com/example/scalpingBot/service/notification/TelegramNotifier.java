package com.example.scalpingBot.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Реализация NotificationService для отправки уведомлений в Telegram.
 */
@Service
public class TelegramNotifier implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotifier.class);
    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot";

    private final RestTemplate restTemplate;
    private final String botToken;
    private final String chatId;
    private final boolean enabled;

    @Autowired
    public TelegramNotifier(RestTemplate restTemplate,
                            @Value("${telegram.bot-token:}") String botToken,
                            @Value("${telegram.chat-id:}") String chatId) {
        this.restTemplate = restTemplate;
        this.botToken = botToken;
        this.chatId = chatId;
        // Уведомления включены, только если заданы и токен, и chat-id
        this.enabled = botToken != null && !botToken.isEmpty() && chatId != null && !chatId.isEmpty();
        if (enabled) {
            log.info("Telegram Notifier is ENABLED for chat ID: {}", chatId);
        } else {
            log.warn("Telegram Notifier is DISABLED. Please provide 'telegram.bot-token' and 'telegram.chat-id' properties to enable it.");
        }
    }

    @Override
    public void sendInfo(String message) {
        String formattedMessage = "ℹ️ [INFO]\n" + message;
        sendMessage(formattedMessage);
    }

    @Override
    public void sendSuccess(String message) {
        String formattedMessage = "✅ [SUCCESS]\n" + message;
        sendMessage(formattedMessage);
    }

    @Override
    public void sendWarning(String message) {
        String formattedMessage = "⚠️ [WARNING]\n" + message;
        sendMessage(formattedMessage);
    }

    @Override
    public void sendError(String message, Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append("❌ [ERROR]\n").append(message);
        if (throwable != null) {
            sb.append("\n\nException: ").append(throwable.getClass().getSimpleName());
            sb.append("\nMessage: ").append(throwable.getMessage());
        }
        sendMessage(sb.toString());
    }

    /**
     * Основной метод, отправляющий HTTP-запрос в API Telegram.
     * @param text Текст сообщения для отправки.
     */
    private void sendMessage(String text) {
        if (!enabled) {
            log.debug("Skipping Telegram notification because it is disabled. Message: {}", text);
            return;
        }

        String url = TELEGRAM_API_URL + botToken + "/sendMessage";

        // Используем UriComponentsBuilder для корректного кодирования параметров
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("chat_id", this.chatId)
                .queryParam("text", text)
                .queryParam("parse_mode", "Markdown"); // Позволяет использовать базовое форматирование

        try {
            restTemplate.getForObject(builder.toUriString(), String.class);
            log.debug("Successfully sent notification to Telegram.");
        } catch (Exception e) {
            log.error("Failed to send notification to Telegram: {}", e.getMessage());
            // Мы не пробрасываем исключение дальше, так как сбой уведомлений
            // не должен останавливать основную логику бота.
        }
    }
}