package com.example.scalpingBot.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

/**
 * Конфигурация торговых настроек скальпинг-бота
 *
 * Основные параметры:
 * - Агрессивная стратегия скальпинга (0.8%/0.4%)
 * - Анализ каждые 15 секунд
 * - Максимум 1 час в позиции
 * - Поддержка paper trading для обучения
 *
 * @author ScalpingBot Team
 * @version 1.0
 */
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "scalping")
@Validated
@Data
public class TradingConfig {

    /**
     * Основные настройки скальпинга
     */
    @NotNull(message = "Scalping enabled flag is required")
    private Boolean enabled = true;

    @NotNull(message = "Paper trading flag is required")
    private Boolean paperTrading = true;

    /**
     * Параметры торговой стратегии
     */
    private Strategy strategy = new Strategy();

    /**
     * Торговые пары для скальпинга
     */
    @NotEmpty(message = "Trading pairs list cannot be empty")
    @Size(min = 1, max = 20, message = "Trading pairs must be between 1 and 20")
    private List<String> tradingPairs = List.of(
            "BTCUSDT", "ETHUSDT", "ADAUSDT", "DOTUSDT",
            "LINKUSDT", "BNBUSDT", "SOLUSDT", "AVAXUSDT"
    );

    /**
     * Временные рамки торговли
     */
    private TradingHours tradingHours = new TradingHours();

    /**
     * Внутренний класс для параметров стратегии
     */
    @Data
    @Validated
    public static class Strategy {

        /**
         * Целевая прибыль в процентах (0.8% для агрессивного скальпинга)
         */
        @NotNull(message = "Target profit percent is required")
        @DecimalMin(value = "0.1", message = "Target profit must be at least 0.1%")
        @DecimalMax(value = "5.0", message = "Target profit must not exceed 5.0%")
        private BigDecimal targetProfitPercent = new BigDecimal("0.8");

        /**
         * Стоп-лосс в процентах (0.4% для соотношения риск/прибыль 1:2)
         */
        @NotNull(message = "Stop loss percent is required")
        @DecimalMin(value = "0.1", message = "Stop loss must be at least 0.1%")
        @DecimalMax(value = "2.0", message = "Stop loss must not exceed 2.0%")
        private BigDecimal stopLossPercent = new BigDecimal("0.4");

        /**
         * Максимальное время удержания позиции в минутах (60 мин = 1 час)
         */
        @NotNull(message = "Max position time is required")
        @Min(value = 1, message = "Max position time must be at least 1 minute")
        @Max(value = 240, message = "Max position time must not exceed 240 minutes")
        private Integer maxPositionTimeMinutes = 60;

        /**
         * Интервал анализа рынка в секундах (15 секунд для скальпинга)
         */
        @NotNull(message = "Analysis interval is required")
        @Min(value = 5, message = "Analysis interval must be at least 5 seconds")
        @Max(value = 300, message = "Analysis interval must not exceed 300 seconds")
        private Integer analysisIntervalSeconds = 15;

        /**
         * Минимальный объем торгов для входа в позицию (в USDT)
         */
        @NotNull(message = "Minimum volume is required")
        @DecimalMin(value = "10.0", message = "Minimum volume must be at least $10")
        private BigDecimal minimumVolumeUsdt = new BigDecimal("50.0");

        /**
         * Максимальный спред между bid/ask в процентах
         */
        @NotNull(message = "Maximum spread is required")
        @DecimalMin(value = "0.01", message = "Maximum spread must be at least 0.01%")
        @DecimalMax(value = "1.0", message = "Maximum spread must not exceed 1.0%")
        private BigDecimal maxSpreadPercent = new BigDecimal("0.1");
    }

    /**
     * Внутренний класс для настройки торговых часов
     */
    @Data
    @Validated
    public static class TradingHours {

        /**
         * Включены ли ограничения по времени торговли
         * false = торгуем 24/7 (крипторынок)
         */
        private Boolean enabled = false;

        /**
         * Час начала торговли (UTC+3 Moscow)
         */
        @Min(value = 0, message = "Start hour must be between 0 and 23")
        @Max(value = 23, message = "Start hour must be between 0 and 23")
        private Integer startHour = 9;

        /**
         * Час окончания торговли (UTC+3 Moscow)
         */
        @Min(value = 0, message = "End hour must be between 0 and 23")
        @Max(value = 23, message = "End hour must be between 0 and 23")
        private Integer endHour = 21;

        /**
         * Проверка, находимся ли мы в торговых часах
         */
        public boolean isWithinTradingHours() {
            if (!enabled) {
                return true; // Торгуем 24/7
            }

            LocalTime now = LocalTime.now();
            LocalTime start = LocalTime.of(startHour, 0);
            LocalTime end = LocalTime.of(endHour, 0);

            return now.isAfter(start) && now.isBefore(end);
        }
    }

    /**
     * Bean для валидации торговой конфигурации при запуске
     */
    @Bean
    public TradingConfigValidator tradingConfigValidator() {
        return new TradingConfigValidator(this);
    }

    /**
     * Валидатор торговой конфигурации
     */
    public static class TradingConfigValidator {
        private final TradingConfig config;

        public TradingConfigValidator(TradingConfig config) {
            this.config = config;
            validateConfiguration();
        }

        private void validateConfiguration() {
            log.info("Validating trading configuration...");

            // Проверка соотношения риск/прибыль
            BigDecimal riskRewardRatio = config.strategy.targetProfitPercent
                    .divide(config.strategy.stopLossPercent, 2, BigDecimal.ROUND_HALF_UP);

            if (riskRewardRatio.compareTo(new BigDecimal("1.5")) < 0) {
                log.warn("Risk/Reward ratio is less than 1.5:1. Current: {}:1", riskRewardRatio);
            }

            // Проверка торговых пар
            if (config.tradingPairs.isEmpty()) {
                throw new IllegalArgumentException("At least one trading pair must be configured");
            }

            // Валидация торговых пар (должны содержать USDT)
            config.tradingPairs.forEach(pair -> {
                if (!pair.endsWith("USDT")) {
                    log.warn("Trading pair {} does not end with USDT", pair);
                }
            });

            // Проверка paper trading в prod
            String activeProfile = System.getProperty("spring.profiles.active", "dev");
            if ("prod".equals(activeProfile) && config.paperTrading) {
                log.warn("⚠️ PAPER TRADING IS ENABLED IN PRODUCTION PROFILE!");
            }

            logConfiguration();
        }

        private void logConfiguration() {
            log.info("========================================");
            log.info("TRADING CONFIGURATION LOADED:");
            log.info("========================================");
            log.info("Scalping enabled: {}", config.enabled);
            log.info("Paper trading: {}", config.paperTrading);
            log.info("Target profit: {}%", config.strategy.targetProfitPercent);
            log.info("Stop loss: {}%", config.strategy.stopLossPercent);
            log.info("Risk/Reward ratio: {}:1",
                    config.strategy.targetProfitPercent.divide(
                            config.strategy.stopLossPercent, 2, BigDecimal.ROUND_HALF_UP));
            log.info("Max position time: {} minutes", config.strategy.maxPositionTimeMinutes);
            log.info("Analysis interval: {} seconds", config.strategy.analysisIntervalSeconds);
            log.info("Minimum volume: ${}", config.strategy.minimumVolumeUsdt);
            log.info("Max spread: {}%", config.strategy.maxSpreadPercent);
            log.info("Trading pairs ({}): {}", config.tradingPairs.size(), config.tradingPairs);
            log.info("Trading hours enabled: {}", config.tradingHours.enabled);

            if (config.tradingHours.enabled) {
                log.info("Trading hours: {:02d}:00 - {:02d}:00 (Moscow)",
                        config.tradingHours.startHour, config.tradingHours.endHour);
            } else {
                log.info("Trading hours: 24/7 (cryptocurrency market)");
            }
            log.info("========================================");
        }
    }

    /**
     * Получить соотношение риск/прибыль
     */
    public BigDecimal getRiskRewardRatio() {
        return strategy.targetProfitPercent.divide(
                strategy.stopLossPercent, 2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Проверить, разрешена ли торговля в текущее время
     */
    public boolean isTradingAllowed() {
        return enabled && tradingHours.isWithinTradingHours();
    }

    /**
     * Получить количество анализов в час
     */
    public int getAnalysisPerHour() {
        return 3600 / strategy.analysisIntervalSeconds;
    }

    /**
     * Проверить, является ли пара валидной для торговли
     */
    public boolean isValidTradingPair(String pair) {
        return tradingPairs.contains(pair.toUpperCase());
    }
}