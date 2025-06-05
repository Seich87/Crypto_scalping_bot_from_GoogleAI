package com.example.scalpingBot.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.*;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Конфигурация системы планировщиков для скальпинг-бота

 * Основные задачи:
 * - Торговый анализ каждые 15 секунд
 * - Мониторинг рисков каждые 5 секунд
 * - Сбор рыночных данных каждую минуту
 * - Сброс дневной статистики в полночь
 * - Очистка старых данных
 *
 * @author ScalpingBot Team
 * @version 1.0
 */
@Slf4j
@Configuration
@EnableScheduling
@ConfigurationProperties(prefix = "scheduler")
@Validated
@Data
public class SchedulingConfig {

    /**
     * Размер пула потоков для планировщика
     */
    @Min(value = 5, message = "Thread pool size must be at least 5")
    @Max(value = 50, message = "Thread pool size must not exceed 50")
    private Integer threadPoolSize = 10;

    /**
     * Настройки отдельных задач
     */
    private Tasks tasks = new Tasks();

    /**
     * Московский часовой пояс для всех планировщиков
     */
    private static final String MOSCOW_TIMEZONE = "Europe/Moscow";

    /**
     * Внутренний класс для настроек задач
     */
    @Data
    @Validated
    public static class Tasks {

        /**
         * Настройки анализа торговли
         */
        private TradingAnalysis tradingAnalysis = new TradingAnalysis();

        /**
         * Настройки мониторинга рисков
         */
        private RiskMonitoring riskMonitoring = new RiskMonitoring();

        /**
         * Настройки сбора рыночных данных
         */
        private MarketDataCollection marketDataCollection = new MarketDataCollection();

        /**
         * Настройки ежедневного сброса
         */
        private DailyReset dailyReset = new DailyReset();

        /**
         * Настройки очистки данных
         */
        private DataCleanup dataCleanup = new DataCleanup();

        /**
         * Настройки мониторинга здоровья системы
         */
        private HealthCheck healthCheck = new HealthCheck();
    }

    /**
     * Задача торгового анализа (каждые 15 секунд)
     */
    @Data
    @Validated
    public static class TradingAnalysis {

        @NotNull(message = "Trading analysis enabled flag is required")
        private Boolean enabled = true;

        @Min(value = 5, message = "Fixed rate must be at least 5 seconds")
        @Max(value = 300, message = "Fixed rate must not exceed 300 seconds")
        private Integer fixedRateSeconds = 15;

        @Min(value = 0, message = "Initial delay cannot be negative")
        private Integer initialDelaySeconds = 30;
    }

    /**
     * Задача мониторинга рисков (каждые 5 секунд)
     */
    @Data
    @Validated
    public static class RiskMonitoring {

        @NotNull(message = "Risk monitoring enabled flag is required")
        private Boolean enabled = true;

        @Min(value = 1, message = "Fixed rate must be at least 1 second")
        @Max(value = 60, message = "Fixed rate must not exceed 60 seconds")
        private Integer fixedRateSeconds = 5;

        @Min(value = 0, message = "Initial delay cannot be negative")
        private Integer initialDelaySeconds = 10;
    }

    /**
     * Задача сбора рыночных данных (каждую минуту)
     */
    @Data
    @Validated
    public static class MarketDataCollection {

        @NotNull(message = "Market data collection enabled flag is required")
        private Boolean enabled = true;

        @Min(value = 30, message = "Fixed rate must be at least 30 seconds")
        @Max(value = 3600, message = "Fixed rate must not exceed 3600 seconds")
        private Integer fixedRateSeconds = 60;

        @Min(value = 0, message = "Initial delay cannot be negative")
        private Integer initialDelaySeconds = 60;
    }

    /**
     * Задача ежедневного сброса (в полночь по Москве)
     */
    @Data
    @Validated
    public static class DailyReset {

        @NotNull(message = "Daily reset enabled flag is required")
        private Boolean enabled = true;

        @NotBlank(message = "Cron expression is required")
        @Pattern(regexp = "^[0-59] [0-59] [0-23] \\* \\* \\?$",
                message = "Invalid cron expression format")
        private String cron = "0 0 0 * * ?"; // Каждый день в полночь
    }

    /**
     * Задача очистки старых данных (каждый час)
     */
    @Data
    @Validated
    public static class DataCleanup {

        @NotNull(message = "Data cleanup enabled flag is required")
        private Boolean enabled = true;

        @NotBlank(message = "Cron expression is required")
        private String cron = "0 0 * * * ?"; // Каждый час

        @Min(value = 1, message = "Data retention days must be at least 1")
        @Max(value = 365, message = "Data retention days must not exceed 365")
        private Integer dataRetentionDays = 90;
    }

    /**
     * Задача проверки здоровья системы (каждые 30 секунд)
     */
    @Data
    @Validated
    public static class HealthCheck {

        @NotNull(message = "Health check enabled flag is required")
        private Boolean enabled = true;

        @Min(value = 10, message = "Fixed rate must be at least 10 seconds")
        @Max(value = 300, message = "Fixed rate must not exceed 300 seconds")
        private Integer fixedRateSeconds = 30;

        @Min(value = 0, message = "Initial delay cannot be negative")
        private Integer initialDelaySeconds = 60;
    }

    /**
     * Основной планировщик задач с оптимизированным пулом потоков
     */
    @Bean(name = "taskScheduler")
    public TaskScheduler taskScheduler() {
        log.info("Configuring Task Scheduler with {} threads", threadPoolSize);

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        // Основные настройки пула
        scheduler.setPoolSize(threadPoolSize);
        scheduler.setThreadNamePrefix("ScalpingBot-Scheduler-");
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);

        // Примечание: Московский часовой пояс устанавливается глобально в DatabaseConfig

        // Обработчик отклоненных задач
        scheduler.setRejectedExecutionHandler(new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                log.error("Task rejected by scheduler: {}. Pool stats: active={}, queue={}, completed={}",
                        r.getClass().getSimpleName(),
                        executor.getActiveCount(),
                        executor.getQueue().size(),
                        executor.getCompletedTaskCount());

                // Попытка выполнить в текущем потоке (fallback)
                if (!executor.isShutdown()) {
                    try {
                        r.run();
                        log.info("Task executed in fallback mode: {}", r.getClass().getSimpleName());
                    } catch (Exception e) {
                        log.error("Failed to execute task in fallback mode: {}", e.getMessage());
                    }
                }
            }
        });

        // Обработчик необработанных исключений
        scheduler.setErrorHandler(throwable -> {
            log.error("Unhandled error in scheduled task: {}", throwable.getMessage(), throwable);

            // Для критических ошибок - уведомление
            if (throwable instanceof OutOfMemoryError ||
                    throwable instanceof StackOverflowError) {
                log.error("CRITICAL SYSTEM ERROR detected in scheduler: {}", throwable.getClass().getSimpleName());
                // Здесь можно добавить отправку критических уведомлений
            }
        });

        // Инициализация планировщика
        scheduler.initialize();

        log.info("Task Scheduler configured successfully:");
        log.info("- Pool size: {}", threadPoolSize);
        log.info("- Timezone: {}", MOSCOW_TIMEZONE);
        log.info("- Await termination: 60s");
        log.info("- Custom rejection handler: enabled");
        log.info("- Custom error handler: enabled");

        return scheduler;
    }

    /**
     * Bean для валидации конфигурации планировщика
     */
    @Bean
    public SchedulerConfigValidator schedulerConfigValidator() {
        return new SchedulerConfigValidator(this);
    }

    /**
     * Валидатор конфигурации планировщика
     */
    public static class SchedulerConfigValidator {
        private final SchedulingConfig config;

        public SchedulerConfigValidator(SchedulingConfig config) {
            this.config = config;
            validateConfiguration();
        }

        private void validateConfiguration() {
            log.info("Validating scheduler configuration...");

            // Проверка логики интервалов
            validateTaskIntervals();

            // Проверка ресурсов
            validateResourceUsage();

            // Проверка cron выражений
            validateCronExpressions();

            logConfiguration();
        }

        private void validateTaskIntervals() {
            // Риск-мониторинг должен быть чаще торгового анализа
            if (config.tasks.riskMonitoring.fixedRateSeconds >=
                    config.tasks.tradingAnalysis.fixedRateSeconds) {
                log.warn("⚠️ Risk monitoring interval should be shorter than trading analysis interval");
            }

            // Проверка разумности интервалов
            if (config.tasks.tradingAnalysis.fixedRateSeconds < 10) {
                log.warn("⚠️ Very frequent trading analysis may cause high CPU usage");
            }

            if (config.tasks.riskMonitoring.fixedRateSeconds < 3) {
                log.warn("⚠️ Very frequent risk monitoring may cause high CPU usage");
            }
        }

        private void validateResourceUsage() {
            // Расчет предполагаемого количества задач в минуту
            int tasksPerMinute = 0;

            if (config.tasks.tradingAnalysis.enabled) {
                tasksPerMinute += 60 / config.tasks.tradingAnalysis.fixedRateSeconds;
            }

            if (config.tasks.riskMonitoring.enabled) {
                tasksPerMinute += 60 / config.tasks.riskMonitoring.fixedRateSeconds;
            }

            if (config.tasks.marketDataCollection.enabled) {
                tasksPerMinute += 60 / config.tasks.marketDataCollection.fixedRateSeconds;
            }

            if (config.tasks.healthCheck.enabled) {
                tasksPerMinute += 60 / config.tasks.healthCheck.fixedRateSeconds;
            }

            log.info("Estimated tasks per minute: {}", tasksPerMinute);

            if (tasksPerMinute > 100) {
                log.warn("⚠️ High task frequency detected: {} tasks/minute. Consider optimizing intervals.",
                        tasksPerMinute);
            }

            // Проверка размера пула потоков
            if (config.threadPoolSize < 5) {
                log.warn("⚠️ Small thread pool size may cause task delays");
            }
        }

        private void validateCronExpressions() {
            // Базовая валидация cron выражений
            try {
                if (config.tasks.dailyReset.enabled) {
                    String cron = config.tasks.dailyReset.cron;
                    if (!cron.matches("^[0-59] [0-59] [0-23] \\* \\* \\?$")) {
                        log.warn("⚠️ Daily reset cron expression may be invalid: {}", cron);
                    }
                }

                if (config.tasks.dataCleanup.enabled) {
                    String cron = config.tasks.dataCleanup.cron;
                    if (cron.isEmpty()) {
                        throw new IllegalArgumentException("Data cleanup cron expression cannot be empty");
                    }
                }
            } catch (Exception e) {
                log.error("Invalid cron expression detected: {}", e.getMessage());
                throw new IllegalArgumentException("Invalid scheduler configuration", e);
            }
        }

        private void logConfiguration() {
            log.info("========================================");
            log.info("SCHEDULER CONFIGURATION:");
            log.info("========================================");
            log.info("Thread pool size: {}", config.threadPoolSize);
            log.info("Timezone: {}", MOSCOW_TIMEZONE);

            log.info("========================================");
            log.info("SCHEDULED TASKS:");
            log.info("========================================");

            if (config.tasks.tradingAnalysis.enabled) {
                log.info("✅ Trading Analysis: every {}s (initial delay: {}s)",
                        config.tasks.tradingAnalysis.fixedRateSeconds,
                        config.tasks.tradingAnalysis.initialDelaySeconds);
            } else {
                log.info("❌ Trading Analysis: DISABLED");
            }

            if (config.tasks.riskMonitoring.enabled) {
                log.info("✅ Risk Monitoring: every {}s (initial delay: {}s)",
                        config.tasks.riskMonitoring.fixedRateSeconds,
                        config.tasks.riskMonitoring.initialDelaySeconds);
            } else {
                log.info("❌ Risk Monitoring: DISABLED");
            }

            if (config.tasks.marketDataCollection.enabled) {
                log.info("✅ Market Data Collection: every {}s (initial delay: {}s)",
                        config.tasks.marketDataCollection.fixedRateSeconds,
                        config.tasks.marketDataCollection.initialDelaySeconds);
            } else {
                log.info("❌ Market Data Collection: DISABLED");
            }

            if (config.tasks.healthCheck.enabled) {
                log.info("✅ Health Check: every {}s (initial delay: {}s)",
                        config.tasks.healthCheck.fixedRateSeconds,
                        config.tasks.healthCheck.initialDelaySeconds);
            } else {
                log.info("❌ Health Check: DISABLED");
            }

            if (config.tasks.dailyReset.enabled) {
                log.info("✅ Daily Reset: cron '{}' (Moscow time)", config.tasks.dailyReset.cron);
            } else {
                log.info("❌ Daily Reset: DISABLED");
            }

            if (config.tasks.dataCleanup.enabled) {
                log.info("✅ Data Cleanup: cron '{}' (retention: {} days)",
                        config.tasks.dataCleanup.cron, config.tasks.dataCleanup.dataRetentionDays);
            } else {
                log.info("❌ Data Cleanup: DISABLED");
            }

            log.info("========================================");
        }
    }

    /**
     * Получить общее количество активных задач
     */
    public int getActiveTasksCount() {
        int count = 0;
        if (tasks.tradingAnalysis.enabled) count++;
        if (tasks.riskMonitoring.enabled) count++;
        if (tasks.marketDataCollection.enabled) count++;
        if (tasks.healthCheck.enabled) count++;
        if (tasks.dailyReset.enabled) count++;
        if (tasks.dataCleanup.enabled) count++;
        return count;
    }

    /**
     * Получить расчетную нагрузку планировщика (задач в минуту)
     */
    public int getEstimatedTasksPerMinute() {
        int tasksPerMinute = 0;

        if (tasks.tradingAnalysis.enabled) {
            tasksPerMinute += 60 / tasks.tradingAnalysis.fixedRateSeconds;
        }

        if (tasks.riskMonitoring.enabled) {
            tasksPerMinute += 60 / tasks.riskMonitoring.fixedRateSeconds;
        }

        if (tasks.marketDataCollection.enabled) {
            tasksPerMinute += 60 / tasks.marketDataCollection.fixedRateSeconds;
        }

        if (tasks.healthCheck.enabled) {
            tasksPerMinute += 60 / tasks.healthCheck.fixedRateSeconds;
        }

        return tasksPerMinute;
    }
}