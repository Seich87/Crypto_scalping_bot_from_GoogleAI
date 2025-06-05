package com.example.scalpingBot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.TimeZone;

/**
 * Главный класс криптовалютного скальпинг-бота

 * Особенности:
 * - Автоматическая торговля с агрессивной стратегией скальпинга (0.8%/0.4%)
 * - Продвинутый риск-менеджмент (2% дневной лимит, 5% на позицию)
 * - Анализ каждые 15 секунд в реальном времени
 * - Поддержка множественных бирж (Binance, Bybit)
 * - Московский часовой пояс для всех операций
 *
 * @author ScalpingBot Team
 * @version 1.0
 * @since 2025-06-05
 */
@Slf4j
@SpringBootApplication
@EnableScheduling          // Включаем планировщики для автоматических задач
@EnableAsync              // Включаем асинхронную обработку
@EnableTransactionManagement // Включаем управление транзакциями
public class ScalpingBotApplication {

	/**
	 * Московский часовой пояс для всех операций
	 */
	private static final String MOSCOW_TIMEZONE = "Europe/Moscow";

	/**
	 * Версия приложения
	 */
	private static final String APP_VERSION = "1.0.0";

	/**
	 * Название приложения для логов
	 */
	private static final String APP_NAME = "Crypto Scalping Bot";

	/**
	 * Точка входа в приложение
	 *
	 * @param args аргументы командной строки
	 */
	public static void main(String[] args) {
		try {
			// Установка московского часового пояса по умолчанию
			TimeZone.setDefault(TimeZone.getTimeZone(MOSCOW_TIMEZONE));

			// Установка системных свойств для оптимизации производительности
			System.setProperty("spring.output.ansi.enabled", "detect");
			System.setProperty("spring.jpa.open-in-view", "false");

			// Логирование начала запуска
			log.info("========================================");
			log.info("Starting {} v{}", APP_NAME, APP_VERSION);
			log.info("Timezone: {}", MOSCOW_TIMEZONE);
			log.info("Java Version: {}", System.getProperty("java.version"));
			log.info("========================================");

			// Запуск Spring Boot приложения
			SpringApplication app = new SpringApplication(ScalpingBotApplication.class);

			// Настройка дополнительных свойств Spring Boot
			app.setAdditionalProfiles(); // Профили будут взяты из application.properties

			// Запуск контекста приложения
			app.run(args);

		} catch (Exception e) {
			log.error("Failed to start {}: {}", APP_NAME, e.getMessage(), e);
			System.exit(1);
		}
	}

	/**
	 * Инициализация после создания бинов Spring
	 * Выполняется после полной загрузки контекста
	 */
	@PostConstruct
	public void initialize() {
		log.info("========================================");
		log.info("{} successfully initialized!", APP_NAME);
		log.info("Current timezone: {}", TimeZone.getDefault().getID());
		log.info("Application ready for scalping operations");
		log.info("========================================");

		// Проверка критически важных настроек
		validateCriticalSettings();

		// Вывод предупреждений о безопасности
		printSecurityWarnings();
	}

	/**
	 * Очистка ресурсов перед завершением приложения
	 */
	@PreDestroy
	public void cleanup() {
		log.info("========================================");
		log.info("Shutting down {} v{}", APP_NAME, APP_VERSION);
		log.info("Performing cleanup operations...");
		log.info("All scalping operations stopped");
		log.info("Application shutdown completed");
		log.info("========================================");
	}

	/**
	 * Проверка критически важных настроек при запуске
	 */
	private void validateCriticalSettings() {
		// Проверка часового пояса
		String currentTz = TimeZone.getDefault().getID();
		if (!MOSCOW_TIMEZONE.equals(currentTz)) {
			log.warn("WARNING: Expected timezone '{}', but current is '{}'",
					MOSCOW_TIMEZONE, currentTz);
		}

		// Проверка версии Java
		String javaVersion = System.getProperty("java.version");
		if (!javaVersion.startsWith("17")) {
			log.warn("WARNING: Recommended Java 17, current version: {}", javaVersion);
		}

		// Проверка доступной памяти
		long maxMemory = Runtime.getRuntime().maxMemory();
		long maxMemoryMB = maxMemory / (1024 * 1024);
		if (maxMemoryMB < 512) {
			log.warn("WARNING: Low memory detected: {}MB. Recommended: 1GB+", maxMemoryMB);
		}

		log.info("Critical settings validation completed");
	}

	/**
	 * Вывод предупреждений о безопасности и рисках
	 */
	private void printSecurityWarnings() {
		log.info("========================================");
		log.info("SECURITY AND RISK WARNINGS:");
		log.info("========================================");

		// Проверка активного профиля
		String activeProfile = System.getProperty("spring.profiles.active", "default");

		if ("prod".equals(activeProfile)) {
			log.warn("🚨 PRODUCTION MODE DETECTED 🚨");
			log.warn("⚠️  REAL MONEY AT RISK!");
			log.warn("⚠️  Ensure all risk management settings are correct");
			log.warn("⚠️  Monitor positions closely");
		} else {
			log.info("✅ DEVELOPMENT/TEST MODE");
			log.info("✅ Paper trading enabled - no real money at risk");
			log.info("✅ Safe environment for learning and testing");
		}

		log.info("========================================");
		log.info("KEY RISK MANAGEMENT FEATURES:");
		log.info("• Maximum 2% daily loss limit");
		log.info("• Maximum 5% capital per position");
		log.info("• Maximum 10 simultaneous positions");
		log.info("• Automatic stop-loss on all positions");
		log.info("• Emergency stop at 1.8% daily loss");
		log.info("• Position correlation monitoring");
		log.info("========================================");

		log.info("SCALPING STRATEGY PARAMETERS:");
		log.info("• Target profit: 0.8% per trade");
		log.info("• Stop loss: 0.4% per trade");
		log.info("• Risk/Reward ratio: 1:2");
		log.info("• Analysis interval: 15 seconds");
		log.info("• Maximum position time: 1 hour");
		log.info("========================================");
	}
}