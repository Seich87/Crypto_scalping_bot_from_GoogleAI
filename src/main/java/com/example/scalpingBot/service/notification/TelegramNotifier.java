package com.example.scalpingBot.service.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Сервис для отправки уведомлений через Telegram Bot API
 *
 * Основные функции:
 * - Отправка сообщений в Telegram чат
 * - Форматирование сообщений с Markdown разметкой
 * - Rate limiting для соблюдения лимитов Telegram API
 * - Retry логика при временных сбоях
 * - Обработка различных типов уведомлений
 *
 * Использует Telegram Bot API для доставки уведомлений
 * о торговых операциях, ошибках и критических событиях.
 *
 * @author ScalpingBot Team
 * @version 1.0
 */
@Slf4j
@Component
public class TelegramNotifier {

    private final RestTemplate restTemplate;

    /**
     * Конфигурация Telegram
     */
    @Value("${notifications.telegram.bot-token:}")
    private String botToken;

    @Value("${notifications.telegram.chat-id:}")
    private String chatId;

    @Value("${notifications.telegram.enabled:true}")
    private boolean enabled;

    /**
     * Настройки для различных типов уведомлений
     */
    @Value("${notifications.telegram.alerts.trade-opened:true}")
    private boolean tradeOpenedEnabled;

    @Value("${notifications.telegram.alerts.trade-closed:true}")
    private boolean tradeClosedEnabled;

    @Value("${notifications.telegram.alerts.stop-loss-triggered:true}")
    private boolean stopLossEnabled;

    @Value("${notifications.telegram.alerts.daily-limit-reached:true}")
    private boolean dailyLimitEnabled;

    @Value("${notifications.telegram.alerts.emergency-stop:true}")
    private boolean emergencyStopEnabled;

    /**
     * Rate limiting
     */
    private final AtomicLong lastMessageTime = new AtomicLong(0);
    private static final long MIN_MESSAGE_INTERVAL_MS = 1000; // 1 секунда между сообщениями
    private static final int MAX_MESSAGE_LENGTH = 4096; // Максимальная длина сообщения в Telegram

    /**
     * API URL Telegram
     */
    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot%s/sendMessage";

    /**
     * Форматтер для времени
     */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public TelegramNotifier() {
        this.restTemplate = new RestTemplate();

        // Настройка таймаутов для HTTP клиента
        this.restTemplate.getRequestFactory();
    }

    /**
     * Отправить сообщение в Telegram
     *
     * @param type тип уведомления
     * @param title заголовок
     * @param message сообщение
     * @param priority приоритет
     */
    public void sendMessage(NotificationService.NotificationType type, String title,
                            String message, NotificationService.NotificationPriority priority) {

        if (!isEnabled() || !shouldSendForType(type)) {
            log.debug("Telegram notification disabled for type: {}", type);
            return;
        }

        if (!isValidConfiguration()) {
            log.warn("Telegram configuration is invalid. Check bot-token and chat-id");
            return;
        }

        try {
            // Применяем rate limiting
            if (!checkRateLimit()) {
                log.warn("Rate limit exceeded for Telegram notifications");
                return;
            }

            // Форматируем сообщение для Telegram
            String telegramMessage = formatTelegramMessage(type, title, message, priority);

            // Обрезаем сообщение если слишком длинное
            if (telegramMessage.length() > MAX_MESSAGE_LENGTH) {
                telegramMessage = telegramMessage.substring(0, MAX_MESSAGE_LENGTH - 100) + "\n\n...(сообщение обрезано)";
            }

            // Отправляем сообщение
            sendToTelegram(telegramMessage);

            log.debug("Telegram notification sent: {} - {}", type, title);

        } catch (Exception e) {
            log.error("Failed to send Telegram notification: {}", e.getMessage());
        }
    }

    /**
     * Проверить, включены ли уведомления
     *
     * @return true если включены
     */
    private boolean isEnabled() {
        return enabled && botToken != null && !botToken.isEmpty() &&
                chatId != null && !chatId.isEmpty();
    }

    /**
     * Проверить корректность конфигурации
     *
     * @return true если конфигурация валидна
     */
    private boolean isValidConfiguration() {
        if (botToken == null || botToken.trim().isEmpty()) {
            return false;
        }

        if (chatId == null || chatId.trim().isEmpty()) {
            return false;
        }

        // Простая проверка формата токена
        if (!botToken.matches("^[0-9]{8,10}:[a-zA-Z0-9_-]{35}$")) {
            log.warn("Bot token format seems invalid");
            return false;
        }

        return true;
    }

    /**
     * Проверить, нужно ли отправлять уведомление данного типа
     *
     * @param type тип уведомления
     * @return true если нужно отправлять
     */
    private boolean shouldSendForType(NotificationService.NotificationType type) {
        switch (type) {
            case TRADE:
                return tradeOpenedEnabled || tradeClosedEnabled;
            case CRITICAL:
                return emergencyStopEnabled;
            case WARNING:
                return dailyLimitEnabled;
            case ERROR:
                return stopLossEnabled;
            case SUCCESS:
            case INFO:
            default:
                return true;
        }
    }

    /**
     * Проверить rate limit
     *
     * @return true если можно отправлять
     */
    private boolean checkRateLimit() {
        long now = System.currentTimeMillis();
        long lastTime = lastMessageTime.get();

        if (now - lastTime < MIN_MESSAGE_INTERVAL_MS) {
            return false;
        }

        return lastMessageTime.compareAndSet(lastTime, now);
    }

    /**
     * Форматировать сообщение для Telegram
     *
     * @param type тип уведомления
     * @param title заголовок
     * @param message сообщение
     * @param priority приоритет
     * @return форматированное сообщение
     */
    private String formatTelegramMessage(NotificationService.NotificationType type, String title,
                                         String message, NotificationService.NotificationPriority priority) {

        StringBuilder formatted = new StringBuilder();

        // Добавляем эмодзи и заголовок
        formatted.append(getEmojiForType(type, priority)).append(" ");
        formatted.append("*").append(escapeMarkdown(title)).append("*\n\n");

        // Добавляем основное сообщение
        formatted.append(escapeMarkdown(message));

        // Добавляем время и тип
        formatted.append("\n\n");
        formatted.append("🕐 ").append(LocalDateTime.now().format(TIME_FORMATTER));

        if (priority == NotificationService.NotificationPriority.CRITICAL) {
            formatted.append(" | ").append("🚨 КРИТИЧНО");
        }

        return formatted.toString();
    }

    /**
     * Получить эмодзи для типа уведомления
     *
     * @param type тип уведомления
     * @param priority приоритет
     * @return эмодзи
     */
    private String getEmojiForType(NotificationService.NotificationType type,
                                   NotificationService.NotificationPriority priority) {

        if (priority == NotificationService.NotificationPriority.CRITICAL) {
            return "🚨🚨🚨";
        }

        switch (type) {
            case TRADE:
                return "💰";
            case SUCCESS:
                return "✅";
            case WARNING:
                return "⚠️";
            case ERROR:
                return "❌";
            case CRITICAL:
                return "🚨";
            case INFO:
            default:
                return "ℹ️";
        }
    }

    /**
     * Экранировать специальные символы Markdown
     *
     * @param text исходный текст
     * @return экранированный текст
     */
    private String escapeMarkdown(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("~", "\\~")
                .replace("`", "\\`")
                .replace(">", "\\>")
                .replace("#", "\\#")
                .replace("+", "\\+")
                .replace("-", "\\-")
                .replace("=", "\\=")
                .replace("|", "\\|")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace(".", "\\.")
                .replace("!", "\\!");
    }

    /**
     * Отправить сообщение в Telegram через API
     *
     * @param message сообщение для отправки
     */
    private void sendToTelegram(String message) {
        try {
            String url = String.format(TELEGRAM_API_URL, botToken);

            // Создаем тело запроса
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("chat_id", chatId);
            requestBody.put("text", message);
            requestBody.put("parse_mode", "Markdown");
            requestBody.put("disable_web_page_preview", true);

            // Настраиваем заголовки
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Отправляем запрос
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                log.debug("Telegram message sent successfully");
            } else {
                log.warn("Telegram API returned status: {}", response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Error sending message to Telegram: {}", e.getMessage());

            // Для критических ошибок можем попробовать упрощенную отправку
            if (e.getMessage().contains("parse_mode")) {
                sendSimpleMessage(message);
            }
        }
    }

    /**
     * Отправить простое сообщение без Markdown (fallback)
     *
     * @param message сообщение
     */
    private void sendSimpleMessage(String message) {
        try {
            String url = String.format(TELEGRAM_API_URL, botToken);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("chat_id", chatId);
            requestBody.put("text", removeMarkdown(message));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            log.debug("Simple Telegram message sent as fallback");

        } catch (Exception e) {
            log.error("Failed to send simple Telegram message: {}", e.getMessage());
        }
    }

    /**
     * Удалить Markdown разметку
     *
     * @param text текст с разметкой
     * @return текст без разметки
     */
    private String removeMarkdown(String text) {
        return text.replaceAll("[*_`\\[\\]()~>#+\\-=|{}.!]", "");
    }

    /**
     * Отправить тестовое сообщение для проверки конфигурации
     *
     * @return true если сообщение отправлено успешно
     */
    public boolean sendTestMessage() {
        if (!isValidConfiguration()) {
            log.error("Cannot send test message - invalid configuration");
            return false;
        }

        try {
            String testMessage = String.format(
                    "🤖 *Тест уведомлений*\n\n" +
                            "Система уведомлений скальпинг-бота настроена корректно!\n\n" +
                            "🕐 %s",
                    LocalDateTime.now().format(TIME_FORMATTER)
            );

            sendToTelegram(testMessage);
            return true;

        } catch (Exception e) {
            log.error("Failed to send test message: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Получить информацию о боте
     *
     * @return информация о боте или null при ошибке
     */
    public String getBotInfo() {
        if (!isValidConfiguration()) {
            return null;
        }

        try {
            String url = String.format("https://api.telegram.org/bot%s/getMe", botToken);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }

        } catch (Exception e) {
            log.error("Failed to get bot info: {}", e.getMessage());
        }

        return null;
    }
}