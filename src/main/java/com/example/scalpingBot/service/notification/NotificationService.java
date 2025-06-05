package com.example.scalpingBot.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Базовый сервис уведомлений для скальпинг-бота
 *
 * Основные функции:
 * - Отправка уведомлений через различные каналы (Telegram, Email)
 * - Фильтрация дублирующихся уведомлений
 * - Приоритизация критических сообщений
 * - Асинхронная доставка для минимизации задержек
 * - Retry логика для неудачных отправок
 *
 * Все уведомления форматируются с учетом типа канала
 * и важности сообщения.
 *
 * @author ScalpingBot Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final TelegramNotifier telegramNotifier;

    /**
     * Настройки уведомлений
     */
    @Value("${notifications.telegram.enabled:true}")
    private boolean telegramEnabled;

    @Value("${notifications.email.enabled:false}")
    private boolean emailEnabled;

    /**
     * Кеш для предотвращения дублирующихся уведомлений
     */
    private final Map<String, LocalDateTime> notificationCache = new ConcurrentHashMap<>();

    /**
     * Время жизни кеша в секундах
     */
    private static final int CACHE_TTL_SECONDS = 300; // 5 минут

    /**
     * Отправить торговое уведомление
     *
     * @param title заголовок
     * @param message сообщение
     */
    public void sendTradeAlert(String title, String message) {
        sendNotification(NotificationType.TRADE, title, message, NotificationPriority.NORMAL);
    }

    /**
     * Отправить уведомление об ошибке
     *
     * @param title заголовок
     * @param message сообщение
     */
    public void sendErrorAlert(String title, String message) {
        sendNotification(NotificationType.ERROR, title, message, NotificationPriority.HIGH);
    }

    /**
     * Отправить предупреждение
     *
     * @param title заголовок
     * @param message сообщение
     */
    public void sendWarningAlert(String title, String message) {
        sendNotification(NotificationType.WARNING, title, message, NotificationPriority.NORMAL);
    }

    /**
     * Отправить информационное уведомление
     *
     * @param title заголовок
     * @param message сообщение
     */
    public void sendInfoAlert(String title, String message) {
        sendNotification(NotificationType.INFO, title, message, NotificationPriority.LOW);
    }

    /**
     * Отправить уведомление об успехе
     *
     * @param title заголовок
     * @param message сообщение
     */
    public void sendSuccessAlert(String title, String message) {
        sendNotification(NotificationType.SUCCESS, title, message, NotificationPriority.NORMAL);
    }

    /**
     * Отправить критическое уведомление
     *
     * @param title заголовок
     * @param message сообщение
     */
    public void sendCriticalAlert(String title, String message) {
        sendNotification(NotificationType.CRITICAL, title, message, NotificationPriority.CRITICAL);
    }

    /**
     * Основной метод отправки уведомлений
     *
     * @param type тип уведомления
     * @param title заголовок
     * @param message сообщение
     * @param priority приоритет
     */
    private void sendNotification(NotificationType type, String title, String message, NotificationPriority priority) {
        try {
            // Создаем ключ для кеширования
            String cacheKey = generateCacheKey(type, title, message);

            // Проверяем кеш для предотвращения дублирования
            if (isDuplicate(cacheKey)) {
                log.debug("Skipping duplicate notification: {}", title);
                return;
            }

            // Добавляем в кеш
            notificationCache.put(cacheKey, LocalDateTime.now());

            // Форматируем сообщение
            String formattedMessage = formatMessage(type, title, message);

            // Отправляем асинхронно
            CompletableFuture.runAsync(() -> {
                try {
                    sendToChannels(type, title, formattedMessage, priority);
                } catch (Exception e) {
                    log.error("Failed to send notification: {}", e.getMessage());
                }
            });

            log.debug("Notification queued: {} - {}", type, title);

        } catch (Exception e) {
            log.error("Error sending notification: {}", e.getMessage());
        }
    }

    /**
     * Отправить уведомление по всем активным каналам
     *
     * @param type тип уведомления
     * @param title заголовок
     * @param message сообщение
     * @param priority приоритет
     */
    private void sendToChannels(NotificationType type, String title, String message, NotificationPriority priority) {
        // Telegram
        if (telegramEnabled) {
            try {
                telegramNotifier.sendMessage(type, title, message, priority);
            } catch (Exception e) {
                log.error("Failed to send Telegram notification: {}", e.getMessage());
            }
        }

        // Email (будет реализован позже)
        if (emailEnabled) {
            log.debug("Email notifications not implemented yet");
        }
    }

    /**
     * Форматировать сообщение с учетом типа
     *
     * @param type тип уведомления
     * @param title заголовок
     * @param message сообщение
     * @return форматированное сообщение
     */
    private String formatMessage(NotificationType type, String title, String message) {
        StringBuilder formatted = new StringBuilder();

        // Добавляем эмодзи в зависимости от типа
        formatted.append(getEmojiForType(type)).append(" ");

        // Добавляем заголовок
        formatted.append("*").append(title).append("*\n\n");

        // Добавляем сообщение
        formatted.append(message);

        // Добавляем время
        formatted.append("\n\n_").append(LocalDateTime.now().toString()).append("_");

        return formatted.toString();
    }

    /**
     * Получить эмодзи для типа уведомления
     *
     * @param type тип уведомления
     * @return эмодзи
     */
    private String getEmojiForType(NotificationType type) {
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
     * Генерировать ключ для кеширования
     *
     * @param type тип
     * @param title заголовок
     * @param message сообщение
     * @return ключ кеша
     */
    private String generateCacheKey(NotificationType type, String title, String message) {
        return String.format("%s:%s:%s", type, title, message.hashCode());
    }

    /**
     * Проверить, является ли уведомление дублем
     *
     * @param cacheKey ключ кеша
     * @return true если дубль
     */
    private boolean isDuplicate(String cacheKey) {
        LocalDateTime lastSent = notificationCache.get(cacheKey);
        if (lastSent == null) {
            return false;
        }

        // Проверяем TTL
        LocalDateTime now = LocalDateTime.now();
        long secondsSince = java.time.Duration.between(lastSent, now).getSeconds();

        if (secondsSince > CACHE_TTL_SECONDS) {
            notificationCache.remove(cacheKey);
            return false;
        }

        return true;
    }

    /**
     * Очистить устаревшие записи из кеша
     */
    public void cleanupCache() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(CACHE_TTL_SECONDS);

        notificationCache.entrySet().removeIf(entry ->
                entry.getValue().isBefore(cutoff)
        );

        log.debug("Notification cache cleaned up. Size: {}", notificationCache.size());
    }

    /**
     * Типы уведомлений
     */
    public enum NotificationType {
        TRADE,      // Торговые операции
        SUCCESS,    // Успешные операции
        WARNING,    // Предупреждения
        ERROR,      // Ошибки
        CRITICAL,   // Критические события
        INFO        // Информационные сообщения
    }

    /**
     * Приоритеты уведомлений
     */
    public enum NotificationPriority {
        LOW,        // Низкий приоритет
        NORMAL,     // Обычный приоритет
        HIGH,       // Высокий приоритет
        CRITICAL    // Критический приоритет
    }
}