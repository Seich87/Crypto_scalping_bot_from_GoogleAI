package com.example.scalpingBot.utils;

import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.concurrent.TimeUnit;

/**
 * Утилиты для работы с датами и временем в скальпинг-боте
 *
 * Основные функции:
 * - Работа с московским часовым поясом
 * - Конвертация временных меток бирж
 * - Расчеты торговых интервалов
 * - Проверка торговых часов
 * - Форматирование для логов и уведомлений
 *
 * Все операции учитывают специфику криптовалютного рынка (24/7)
 * и требования скальпинг-стратегии (точность до секунд).
 *
 * @author ScalpingBot Team
 * @version 1.0
 */
@Slf4j
public class DateUtils {

    /**
     * Московский часовой пояс (UTC+3)
     */
    public static final ZoneId MOSCOW_ZONE = ZoneId.of("Europe/Moscow");

    /**
     * UTC часовой пояс
     */
    public static final ZoneId UTC_ZONE = ZoneId.of("UTC");

    /**
     * Стандартные форматы дат
     */
    public static final DateTimeFormatter ISO_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    public static final DateTimeFormatter LOG_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter TRADE_TIMESTAMP = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    public static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter TIME_ONLY = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Форматы для различных бирж
     */
    public static final DateTimeFormatter BINANCE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter BYBIT_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    // Приватный конструктор для утилитарного класса
    private DateUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Получить текущее время в московском часовом поясе
     *
     * @return текущее время в Москве
     */
    public static LocalDateTime nowMoscow() {
        return LocalDateTime.now(MOSCOW_ZONE);
    }

    /**
     * Получить текущее время в UTC
     *
     * @return текущее время в UTC
     */
    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(UTC_ZONE);
    }

    /**
     * Получить текущую временную метку Unix в миллисекундах
     *
     * @return Unix timestamp в миллисекундах
     */
    public static long currentTimestampMs() {
        return Instant.now().toEpochMilli();
    }

    /**
     * Получить текущую временную метку Unix в секундах
     *
     * @return Unix timestamp в секундах
     */
    public static long currentTimestampSeconds() {
        return Instant.now().getEpochSecond();
    }

    /**
     * Конвертировать Unix timestamp в LocalDateTime (московское время)
     *
     * @param timestampMs временная метка в миллисекундах
     * @return LocalDateTime в московском часовом поясе
     */
    public static LocalDateTime fromTimestampMs(long timestampMs) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestampMs),
                MOSCOW_ZONE
        );
    }

    /**
     * Конвертировать Unix timestamp в LocalDateTime (UTC)
     *
     * @param timestampMs временная метка в миллисекундах
     * @return LocalDateTime в UTC
     */
    public static LocalDateTime fromTimestampMsUtc(long timestampMs) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestampMs),
                UTC_ZONE
        );
    }

    /**
     * Конвертировать LocalDateTime в Unix timestamp
     *
     * @param dateTime дата и время
     * @param zoneId часовой пояс
     * @return Unix timestamp в миллисекундах
     */
    public static long toTimestampMs(LocalDateTime dateTime, ZoneId zoneId) {
        return dateTime.atZone(zoneId).toInstant().toEpochMilli();
    }

    /**
     * Конвертировать московское время в Unix timestamp
     *
     * @param moscowDateTime дата и время в Москве
     * @return Unix timestamp в миллисекундах
     */
    public static long toTimestampMsMoscow(LocalDateTime moscowDateTime) {
        return toTimestampMs(moscowDateTime, MOSCOW_ZONE);
    }

    /**
     * Конвертировать UTC время в Unix timestamp
     *
     * @param utcDateTime дата и время в UTC
     * @return Unix timestamp в миллисекундах
     */
    public static long toTimestampMsUtc(LocalDateTime utcDateTime) {
        return toTimestampMs(utcDateTime, UTC_ZONE);
    }

    /**
     * Проверить, находимся ли мы в торговых часах
     *
     * @param startHour час начала торговли (0-23)
     * @param endHour час окончания торговли (0-23)
     * @return true если сейчас торговые часы
     */
    public static boolean isWithinTradingHours(int startHour, int endHour) {
        LocalTime now = LocalTime.now(MOSCOW_ZONE);
        LocalTime start = LocalTime.of(startHour, 0);
        LocalTime end = LocalTime.of(endHour, 0);

        if (startHour < endHour) {
            // Обычный случай: 09:00 - 21:00
            return now.isAfter(start) && now.isBefore(end);
        } else {
            // Переход через полночь: 21:00 - 09:00
            return now.isAfter(start) || now.isBefore(end);
        }
    }

    /**
     * Проверить, является ли текущий день торговым
     * (для крипторынка все дни торговые, но может быть полезно для других активов)
     *
     * @return true если сегодня торговый день
     */
    public static boolean isTradingDay() {
        // Для криптовалют все дни торговые (24/7)
        return true;
    }

    /**
     * Проверить, является ли указанная дата торговым днем
     *
     * @param date проверяемая дата
     * @return true если это торговый день
     */
    public static boolean isTradingDay(LocalDate date) {
        // Для криптовалют все дни торговые
        return true;
    }

    /**
     * Получить начало текущего дня в московском времени
     *
     * @return начало дня (00:00:00)
     */
    public static LocalDateTime startOfTodayMoscow() {
        return LocalDate.now(MOSCOW_ZONE).atStartOfDay();
    }

    /**
     * Получить конец текущего дня в московском времени
     *
     * @return конец дня (23:59:59.999)
     */
    public static LocalDateTime endOfTodayMoscow() {
        return LocalDate.now(MOSCOW_ZONE).atTime(LocalTime.MAX);
    }

    /**
     * Получить начало недели (понедельник) в московском времени
     *
     * @return начало текущей недели
     */
    public static LocalDateTime startOfWeekMoscow() {
        return LocalDate.now(MOSCOW_ZONE)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay();
    }

    /**
     * Получить начало месяца в московском времени
     *
     * @return начало текущего месяца
     */
    public static LocalDateTime startOfMonthMoscow() {
        return LocalDate.now(MOSCOW_ZONE)
                .with(TemporalAdjusters.firstDayOfMonth())
                .atStartOfDay();
    }

    /**
     * Рассчитать количество секунд между двумя моментами времени
     *
     * @param from начальное время
     * @param to конечное время
     * @return количество секунд
     */
    public static long secondsBetween(LocalDateTime from, LocalDateTime to) {
        return ChronoUnit.SECONDS.between(from, to);
    }

    /**
     * Рассчитать количество миллисекунд между двумя моментами времени
     *
     * @param from начальное время
     * @param to конечное время
     * @return количество миллисекунд
     */
    public static long millisBetween(LocalDateTime from, LocalDateTime to) {
        return ChronoUnit.MILLIS.between(from, to);
    }

    /**
     * Добавить секунды к дате
     *
     * @param dateTime исходная дата
     * @param seconds количество секунд для добавления
     * @return новая дата
     */
    public static LocalDateTime addSeconds(LocalDateTime dateTime, long seconds) {
        return dateTime.plusSeconds(seconds);
    }

    /**
     * Добавить минуты к дате
     *
     * @param dateTime исходная дата
     * @param minutes количество минут для добавления
     * @return новая дата
     */
    public static LocalDateTime addMinutes(LocalDateTime dateTime, long minutes) {
        return dateTime.plusMinutes(minutes);
    }

    /**
     * Проверить, прошло ли указанное количество времени с момента
     *
     * @param since начальный момент
     * @param amount количество времени
     * @param unit единица времени
     * @return true если время прошло
     */
    public static boolean hasElapsed(LocalDateTime since, long amount, ChronoUnit unit) {
        LocalDateTime now = nowMoscow();
        long elapsed = unit.between(since, now);
        return elapsed >= amount;
    }

    /**
     * Проверить, прошло ли указанное количество секунд
     *
     * @param since начальный момент
     * @param seconds количество секунд
     * @return true если время прошло
     */
    public static boolean hasElapsedSeconds(LocalDateTime since, long seconds) {
        return hasElapsed(since, seconds, ChronoUnit.SECONDS);
    }

    /**
     * Проверить, прошло ли указанное количество минут
     *
     * @param since начальный момент
     * @param minutes количество минут
     * @return true если время прошло
     */
    public static boolean hasElapsedMinutes(LocalDateTime since, long minutes) {
        return hasElapsed(since, minutes, ChronoUnit.MINUTES);
    }

    /**
     * Форматировать дату для логов
     *
     * @param dateTime дата и время
     * @return форматированная строка
     */
    public static String formatForLog(LocalDateTime dateTime) {
        return dateTime.format(LOG_DATETIME);
    }

    /**
     * Форматировать временную метку для торговых операций
     *
     * @param dateTime дата и время
     * @return форматированная строка с миллисекундами
     */
    public static String formatTradeTimestamp(LocalDateTime dateTime) {
        return dateTime.format(TRADE_TIMESTAMP);
    }

    /**
     * Форматировать дату в ISO формате
     *
     * @param dateTime дата и время
     * @return строка в ISO формате
     */
    public static String formatIso(LocalDateTime dateTime) {
        return dateTime.format(ISO_DATETIME);
    }

    /**
     * Парсить дату из строки
     *
     * @param dateString строка с датой
     * @param formatter форматтер
     * @return распарсенная дата или null при ошибке
     */
    public static LocalDateTime parseDateTime(String dateString, DateTimeFormatter formatter) {
        try {
            return LocalDateTime.parse(dateString, formatter);
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse date: {} with formatter: {}", dateString, formatter);
            return null;
        }
    }

    /**
     * Парсить дату в ISO формате
     *
     * @param isoString строка в ISO формате
     * @return распарсенная дата или null при ошибке
     */
    public static LocalDateTime parseIso(String isoString) {
        return parseDateTime(isoString, ISO_DATETIME);
    }

    /**
     * Конвертировать временную метку Binance
     *
     * @param binanceTimestamp временная метка от Binance (миллисекунды)
     * @return LocalDateTime в московском времени
     */
    public static LocalDateTime fromBinanceTimestamp(long binanceTimestamp) {
        return fromTimestampMs(binanceTimestamp);
    }

    /**
     * Конвертировать временную метку Bybit
     *
     * @param bybitTimestamp временная метка от Bybit (секунды или миллисекунды)
     * @return LocalDateTime в московском времени
     */
    public static LocalDateTime fromBybitTimestamp(long bybitTimestamp) {
        // Bybit может возвращать временные метки в секундах или миллисекундах
        // Если число меньше 2000000000000 (примерно 2033 год), то это секунды
        if (bybitTimestamp < 2000000000000L) {
            return fromTimestampMs(bybitTimestamp * 1000);
        } else {
            return fromTimestampMs(bybitTimestamp);
        }
    }

    /**
     * Получить временную метку для запроса к Binance
     *
     * @return текущая временная метка в миллисекундах
     */
    public static long getBinanceTimestamp() {
        return currentTimestampMs();
    }

    /**
     * Получить временную метку для запроса к Bybit
     *
     * @return текущая временная метка в миллисекундах
     */
    public static long getBybitTimestamp() {
        return currentTimestampMs();
    }

    /**
     * Рассчитать время до следующего сброса дневной статистики
     *
     * @return количество миллисекунд до полуночи
     */
    public static long millisecondsUntilMidnight() {
        LocalDateTime now = nowMoscow();
        LocalDateTime nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay();
        return millisBetween(now, nextMidnight);
    }

    /**
     * Рассчитать время до следующего анализа (для планировщика)
     *
     * @param intervalSeconds интервал анализа в секундах
     * @return количество миллисекунд до следующего анализа
     */
    public static long millisecondsUntilNextAnalysis(int intervalSeconds) {
        long currentSeconds = currentTimestampSeconds();
        long nextAnalysisSeconds = ((currentSeconds / intervalSeconds) + 1) * intervalSeconds;
        return (nextAnalysisSeconds - currentSeconds) * 1000;
    }

    /**
     * Проверить, является ли время допустимым для размещения ордера
     * (проверка на выходные для некриптовалютных активов)
     *
     * @param dateTime проверяемое время
     * @return true если время допустимо для торговли
     */
    public static boolean isValidTradingTime(LocalDateTime dateTime) {
        // Для криптовалют торговля идет 24/7
        return true;
    }

    /**
     * Получить читаемую строку времени, прошедшего с момента
     *
     * @param since начальный момент
     * @return строка вида "2м 30с назад"
     */
    public static String getTimeAgoString(LocalDateTime since) {
        LocalDateTime now = nowMoscow();
        long totalSeconds = secondsBetween(since, now);

        if (totalSeconds < 60) {
            return totalSeconds + "с назад";
        }

        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        if (minutes < 60) {
            return String.format("%dм %dс назад", minutes, seconds);
        }

        long hours = minutes / 60;
        minutes = minutes % 60;

        if (hours < 24) {
            return String.format("%dч %dм назад", hours, minutes);
        }

        long days = hours / 24;
        hours = hours % 24;

        return String.format("%dд %dч назад", days, hours);
    }

    /**
     * Проверить, не слишком ли старая временная метка для торговли
     *
     * @param timestamp проверяемая временная метка
     * @param maxAgeSeconds максимальный возраст в секундах
     * @return true если временная метка слишком старая
     */
    public static boolean isTimestampTooOld(LocalDateTime timestamp, long maxAgeSeconds) {
        return hasElapsedSeconds(timestamp, maxAgeSeconds);
    }

    /**
     * Округлить время до ближайшего интервала
     *
     * @param dateTime исходное время
     * @param intervalSeconds интервал в секундах
     * @return округленное время
     */
    public static LocalDateTime roundToInterval(LocalDateTime dateTime, int intervalSeconds) {
        long epochSeconds = dateTime.atZone(MOSCOW_ZONE).toEpochSecond();
        long roundedSeconds = (epochSeconds / intervalSeconds) * intervalSeconds;
        return LocalDateTime.ofEpochSecond(roundedSeconds, 0, ZoneOffset.of("+03:00"));
    }

    /**
     * Конвертировать Duration в читаемую строку
     *
     * @param duration продолжительность
     * @return читаемая строка
     */
    public static String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();

        if (seconds < 60) {
            return seconds + "с";
        }

        long minutes = seconds / 60;
        seconds = seconds % 60;

        if (minutes < 60) {
            return String.format("%dм %dс", minutes, seconds);
        }

        long hours = minutes / 60;
        minutes = minutes % 60;

        return String.format("%dч %dм %dс", hours, minutes, seconds);
    }
}