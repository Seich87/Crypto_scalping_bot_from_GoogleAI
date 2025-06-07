import java.time.Instant

plugins {
	java
	id("org.springframework.boot") version "3.5.0"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// Spring Boot Starters (уже есть)
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")

	// Добавляем для планировщиков и WebSocket
	implementation("org.springframework.boot:spring-boot-starter-websocket")

	// Lombok (уже есть)
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	// База данных
	runtimeOnly("com.mysql:mysql-connector-j")

	// Flyway для миграций БД
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-mysql")

	// HTTP клиенты для API бирж
	implementation("org.springframework.boot:spring-boot-starter-webflux") // Для WebClient
	implementation("com.squareup.okhttp3:okhttp:4.12.0") // Альтернативный HTTP клиент

	// JSON обработка (улучшенная)
	implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("com.fasterxml.jackson.core:jackson-databind")

	// Математические вычисления для технического анализа
	implementation("org.apache.commons:commons-math3:3.6.1")

	// Криптографические операции для API ключей
	implementation("commons-codec:commons-codec:1.16.0")

	// Планировщик задач (улучшенный)
	implementation("org.springframework.boot:spring-boot-starter-quartz")

	// Метрики и мониторинг
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("io.micrometer:micrometer-registry-prometheus") // Для Grafana

	// Кеширование для оптимизации
	implementation("org.springframework.boot:spring-boot-starter-cache")
	implementation("com.github.ben-manes.caffeine:caffeine")

	// Telegram Bot API для уведомлений
	implementation("org.telegram:telegrambots:6.8.0")

	// Конфигурация (для @ConfigurationProperties)
	implementation("org.springframework.boot:spring-boot-configuration-processor")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

	// Инструменты разработки
	developmentOnly("org.springframework.boot:spring-boot-devtools")

	// WebSocket Client
	implementation("org.glassfish.tyrus.bundles:tyrus-standalone-client:2.1.5")

	// Тестирование
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.testcontainers:testcontainers")
	testImplementation("org.testcontainers:mysql")
	testImplementation("org.testcontainers:junit-jupiter")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Конфигурация для testcontainers
dependencyManagement {
	imports {
		mavenBom("org.testcontainers:testcontainers-bom:1.19.0")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()

	// Настройки для тестов
	systemProperty("spring.profiles.active", "test")
	systemProperty("user.timezone", "Europe/Moscow")

	// Увеличиваем память для тестов
	maxHeapSize = "1g"

	// Параллельное выполнение тестов
	maxParallelForks = Runtime.getRuntime().availableProcessors() / 2
}

// Настройки компиляции
tasks.withType<JavaCompile> {
	options.encoding = "UTF-8"
	options.isDeprecation = true
	options.isWarnings = true
}

// Настройки JAR файла
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
	archiveFileName.set("scalping-bot-${version}.jar")

	// Добавляем информацию о сборке
	manifest {
		attributes(mapOf(
				"Implementation-Title" to "Crypto Scalping Bot",
				"Implementation-Version" to version,
				"Built-By" to System.getProperty("user.name"),
				"Built-JDK" to System.getProperty("java.version"),
				"Build-Time" to Instant.now().toString()
		))
	}
}

// Оптимизация для производства
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
	systemProperty("spring.profiles.active", "dev")
	systemProperty("user.timezone", "Europe/Moscow")

	// JVM параметры для разработки
	jvmArgs = listOf(
			"-Xms512m",
			"-Xmx1g",
			"-XX:+UseG1GC",
			"-XX:+UseStringDeduplication"
	)
}