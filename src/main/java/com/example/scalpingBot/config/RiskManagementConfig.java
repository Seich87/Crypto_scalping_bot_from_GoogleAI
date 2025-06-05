package com.example.scalpingBot.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Конфигурация системы риск-менеджмента для скальпинг-бота
 *
 * Жесткие лимиты безопасности:
 * - Максимум 2% потерь в день
 * - Максимум 5% капитала на позицию
 * - Не более 10 позиций одновременно
 * - Аварийная остановка при 1.8% дневных потерь
 * - Контроль корреляции между позициями
 *
 * @author ScalpingBot Team
 * @version 1.0
 */
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "risk")
@Validated
@Data
public class RiskManagementConfig {

    /**
     * Максимальный процент дневных потерь (2% от капитала)
     */
    @NotNull(message = "Max daily loss percent is required")
    @DecimalMin(value = "0.5", message = "Max daily loss must be at least 0.5%")
    @DecimalMax(value = "10.0", message = "Max daily loss must not exceed 10%")
    private BigDecimal maxDailyLossPercent = new BigDecimal("2.0");

    /**
     * Максимальный процент капитала на одну позицию (5%)
     */
    @NotNull(message = "Max position size percent is required")
    @DecimalMin(value = "1.0", message = "Max position size must be at least 1%")
    @DecimalMax(value = "20.0", message = "Max position size must not exceed 20%")
    private BigDecimal maxPositionSizePercent = new BigDecimal("5.0");

    /**
     * Максимальное количество одновременных позиций (10)
     */
    @NotNull(message = "Max simultaneous positions is required")
    @Min(value = 1, message = "Max simultaneous positions must be at least 1")
    @Max(value = 50, message = "Max simultaneous positions must not exceed 50")
    private Integer maxSimultaneousPositions = 10;

    /**
     * Настройки аварийной остановки
     */
    private EmergencyStop emergencyStop = new EmergencyStop();

    /**
     * Настройки корреляционного анализа
     */
    private Correlation correlation = new Correlation();

    /**
     * Дополнительные настройки защиты
     */
    private Protection protection = new Protection();

    /**
     * Внутренний класс для настроек аварийной остановки
     */
    @Data
    @Validated
    public static class EmergencyStop {

        /**
         * Включена ли аварийная остановка
         */
        @NotNull(message = "Emergency stop enabled flag is required")
        private Boolean enabled = true;

        /**
         * Порог потерь для аварийной остановки (1.8% для dev, 1.8% для prod)
         */
        @NotNull(message = "Loss threshold percent is required")
        @DecimalMin(value = "0.5", message = "Loss threshold must be at least 0.5%")
        @DecimalMax(value = "5.0", message = "Loss threshold must not exceed 5%")
        private BigDecimal lossThresholdPercent = new BigDecimal("1.0");

        /**
         * Время блокировки торговли после аварийной остановки (в минутах)
         */
        @Min(value = 5, message = "Cooldown period must be at least 5 minutes")
        @Max(value = 1440, message = "Cooldown period must not exceed 24 hours")
        private Integer cooldownMinutes = 60;

        /**
         * Автоматический перезапуск после cooldown
         */
        private Boolean autoRestart = false;
    }

    /**
     * Внутренний класс для настроек корреляционного анализа
     */
    @Data
    @Validated
    public static class Correlation {

        /**
         * Включен ли корреляционный анализ
         */
        @NotNull(message = "Correlation enabled flag is required")
        private Boolean enabled = true;

        /**
         * Максимальная корреляция между позициями (0.7 = 70%)
         */
        @NotNull(message = "Max correlation is required")
        @DecimalMin(value = "0.1", message = "Max correlation must be at least 0.1")
        @DecimalMax(value = "0.95", message = "Max correlation must not exceed 0.95")
        private BigDecimal maxCorrelation = new BigDecimal("0.7");

        /**
         * Период анализа корреляции в днях
         */
        @Min(value = 7, message = "Analysis period must be at least 7 days")
        @Max(value = 90, message = "Analysis period must not exceed 90 days")
        private Integer analysisPeriodDays = 30;

        /**
         * Минимальное количество точек данных для расчета корреляции
         */
        @Min(value = 50, message = "Min data points must be at least 50")
        private Integer minDataPoints = 100;
    }

    /**
     * Внутренний класс для дополнительных настроек защиты
     */
    @Data
    @Validated
    public static class Protection {

        /**
         * Максимальное количество убыточных сделок подряд
         */
        @Min(value = 3, message = "Max consecutive losses must be at least 3")
        @Max(value = 20, message = "Max consecutive losses must not exceed 20")
        private Integer maxConsecutiveLosses = 5;

        /**
         * Минимальный баланс для продолжения торговли (в USDT)
         */
        @NotNull(message = "Minimum balance is required")
        @DecimalMin(value = "100.0", message = "Minimum balance must be at least $100")
        private BigDecimal minimumBalanceUsdt = new BigDecimal("500.0");

        /**
         * Максимальная просадка портфеля в процентах
         */
        @NotNull(message = "Max drawdown percent is required")
        @DecimalMin(value = "5.0", message = "Max drawdown must be at least 5%")
        @DecimalMax(value = "50.0", message = "Max drawdown must not exceed 50%")
        private BigDecimal maxDrawdownPercent = new BigDecimal("15.0");

        /**
         * Время ожидания между неудачными сделками (в секундах)
         */
        @Min(value = 30, message = "Retry delay must be at least 30 seconds")
        @Max(value = 3600, message = "Retry delay must not exceed 1 hour")
        private Integer retryDelaySeconds = 300; // 5 минут

        /**
         * Включить защиту от волатильности
         */
        private Boolean volatilityProtection = true;

        /**
         * Максимальная волатильность для входа в позицию (ATR в процентах)
         */
        @DecimalMin(value = "1.0", message = "Max volatility must be at least 1%")
        @DecimalMax(value = "20.0", message = "Max volatility must not exceed 20%")
        private BigDecimal maxVolatilityPercent = new BigDecimal("8.0");
    }

    /**
     * Bean для валидации риск-конфигурации при запуске
     */
    @Bean
    public RiskConfigValidator riskConfigValidator() {
        return new RiskConfigValidator(this);
    }

    /**
     * Валидатор риск-конфигурации
     */
    public static class RiskConfigValidator {
        private final RiskManagementConfig config;

        public RiskConfigValidator(RiskManagementConfig config) {
            this.config = config;
            validateConfiguration();
        }

        private void validateConfiguration() {
            log.info("Validating risk management configuration...");

            // Проверка логики аварийной остановки
            if (config.emergencyStop.lossThresholdPercent
                    .compareTo(config.maxDailyLossPercent) >= 0) {
                throw new IllegalArgumentException(
                        "Emergency stop threshold must be less than max daily loss");
            }

            // Проверка разумности лимитов
            if (config.maxPositionSizePercent.multiply(new BigDecimal(config.maxSimultaneousPositions))
                    .compareTo(new BigDecimal("100")) > 0) {
                log.warn("⚠️ WARNING: Max position size * max positions > 100% of capital!");
            }

            // Проверка профиля и настроек
            String activeProfile = System.getProperty("spring.profiles.active", "dev");
            if ("prod".equals(activeProfile)) {
                validateProductionSettings();
            }

            logConfiguration();
        }

        private void validateProductionSettings() {
            // Более строгие проверки для продакшна
            if (config.maxDailyLossPercent.compareTo(new BigDecimal("5.0")) > 0) {
                log.warn("⚠️ PRODUCTION WARNING: Daily loss limit is high: {}%",
                        config.maxDailyLossPercent);
            }

            if (!config.emergencyStop.enabled) {
                throw new IllegalArgumentException(
                        "Emergency stop MUST be enabled in production!");
            }

            if (config.maxSimultaneousPositions > 15) {
                log.warn("⚠️ PRODUCTION WARNING: High number of simultaneous positions: {}",
                        config.maxSimultaneousPositions);
            }
        }

        private void logConfiguration() {
            log.info("========================================");
            log.info("RISK MANAGEMENT CONFIGURATION:");
            log.info("========================================");
            log.info("Max daily loss: {}%", config.maxDailyLossPercent);
            log.info("Max position size: {}%", config.maxPositionSizePercent);
            log.info("Max simultaneous positions: {}", config.maxSimultaneousPositions);
            log.info("Total risk exposure: {}%",
                    config.maxPositionSizePercent.multiply(new BigDecimal(config.maxSimultaneousPositions)));

            log.info("========================================");
            log.info("EMERGENCY STOP SETTINGS:");
            log.info("Emergency stop enabled: {}", config.emergencyStop.enabled);
            log.info("Loss threshold: {}%", config.emergencyStop.lossThresholdPercent);
            log.info("Cooldown period: {} minutes", config.emergencyStop.cooldownMinutes);
            log.info("Auto restart: {}", config.emergencyStop.autoRestart);

            log.info("========================================");
            log.info("CORRELATION ANALYSIS:");
            log.info("Correlation analysis enabled: {}", config.correlation.enabled);
            log.info("Max correlation: {}%", config.correlation.maxCorrelation.multiply(new BigDecimal("100")));
            log.info("Analysis period: {} days", config.correlation.analysisPeriodDays);
            log.info("Min data points: {}", config.correlation.minDataPoints);

            log.info("========================================");
            log.info("ADDITIONAL PROTECTION:");
            log.info("Max consecutive losses: {}", config.protection.maxConsecutiveLosses);
            log.info("Minimum balance: ${}", config.protection.minimumBalanceUsdt);
            log.info("Max drawdown: {}%", config.protection.maxDrawdownPercent);
            log.info("Retry delay: {} seconds", config.protection.retryDelaySeconds);
            log.info("Volatility protection: {}", config.protection.volatilityProtection);
            log.info("Max volatility: {}%", config.protection.maxVolatilityPercent);
            log.info("========================================");
        }
    }

    /**
     * Проверить, можно ли открыть новую позицию с учетом лимитов
     */
    public boolean canOpenNewPosition(int currentPositions, BigDecimal portfolioValue,
                                      BigDecimal positionSize) {
        // Проверка количества позиций
        if (currentPositions >= maxSimultaneousPositions) {
            return false;
        }

        // Проверка размера позиции
        BigDecimal positionPercent = positionSize.divide(portfolioValue, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));

        return positionPercent.compareTo(maxPositionSizePercent) <= 0;
    }

    /**
     * Расчет максимального размера позиции в USDT
     */
    public BigDecimal calculateMaxPositionSize(BigDecimal portfolioValue) {
        return portfolioValue.multiply(maxPositionSizePercent).divide(new BigDecimal("100"));
    }

    /**
     * Проверить, требуется ли аварийная остановка
     */
    public boolean isEmergencyStopRequired(BigDecimal dailyLossPercent) {
        if (!emergencyStop.enabled) {
            return false;
        }

        return dailyLossPercent.abs().compareTo(emergencyStop.lossThresholdPercent) >= 0;
    }
}