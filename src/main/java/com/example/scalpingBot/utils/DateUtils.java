package com.example.scalpingBot.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Класс-утилита для выполнения общих операций с датой и временем.
 * Гарантирует, что все операции со временем в приложении выполняются
 * консистентно и с учетом московского часового пояса.
 */
public final class DateUtils {

    /**
     * Константа для московского часового пояса.
     * Указана в требованиях (timezone - Moscow).
     */
    public static final ZoneId MOSCOW_ZONE = ZoneId.of("Europe/Moscow");

    /**
     * Стандартный форматтер для вывода даты и времени в логах или ответах.
     */
    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Приватный конструктор, чтобы предотвратить создание экземпляров этого класса.
     */
    private DateUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Возвращает текущее время в московском часовом поясе.
     *
     * @return {@link LocalDateTime} с текущим временем в Москве.
     */
    public static LocalDateTime nowInMoscow() {
        return LocalDateTime.now(MOSCOW_ZONE);
    }

    /**
     * Конвертирует временную метку Unix (в миллисекундах) в объект LocalDateTime
     * с учетом московского часового пояса.
     *
     * @param unixMillis временная метка Unix в миллисекундах.
     * @return {@link LocalDateTime} объект.
     */
    public static LocalDateTime fromUnixMillis(long unixMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(unixMillis), MOSCOW_ZONE);
    }

    /**
     * Конвертирует объект LocalDateTime в временную метку Unix (в миллисекундах),
     * предполагая, что исходное время находится в московском часовом поясе.
     *
     * @param dateTime объект LocalDateTime.
     * @return временная метка Unix в миллисекундах.
     */
    public static long toUnixMillis(LocalDateTime dateTime) {
        if (dateTime == null) {
            return 0L;
        }
        return dateTime.atZone(MOSCOW_ZONE).toInstant().toEpochMilli();
    }

    /**
     * Форматирует объект LocalDateTime в строку с использованием стандартного формата.
     *
     * @param dateTime объект LocalDateTime для форматирования.
     * @return строковое представление даты и времени.
     */
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DEFAULT_FORMATTER);
    }
}