package com.example.scalpingBot.service.notification;

/**
 * Интерфейс, определяющий стандартный контракт для отправки уведомлений.
 * Любой сервис-уведомитель (Telegram, Email, SMS) должен реализовать этот интерфейс.
 */
public interface NotificationService {

    /**
     * Отправляет простое информационное сообщение.
     *
     * @param message Текст сообщения.
     */
    void sendInfo(String message);

    /**
     * Отправляет сообщение об успехе (например, о фиксации прибыли).
     * Такие сообщения часто выделяются визуально (например, зеленым цветом).
     *
     * @param message Текст сообщения.
     */
    void sendSuccess(String message);

    /**
     * Отправляет предупреждение.
     * Например, о том, что не удалось получить данные с биржи.
     *
     * @param message Текст сообщения.
     */
    void sendWarning(String message);

    /**
     * Отправляет сообщение об ошибке или критическом сбое.
     * Такие сообщения требуют немедленного внимания.
     *
     * @param message Текст сообщения.
     * @param throwable Исключение, которое вызвало ошибку (может быть null).
     */
    void sendError(String message, Throwable throwable);
}