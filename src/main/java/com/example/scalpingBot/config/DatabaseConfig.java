package com.example.scalpingBot.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Properties;
import java.util.TimeZone;

/**
 * Конфигурация базы данных MySQL для скальпинг-бота
 *
 * Особенности:
 * - Московский часовой пояс для всех временных операций
 * - Оптимизированный пул соединений HikariCP
 * - Настройки производительности для высокочастотной торговли
 * - Автоматическое управление транзакциями
 *
 * @author ScalpingBot Team
 * @version 1.0
 */
@Slf4j
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.example.scalpingBot.repository")
public class DatabaseConfig {

    // MySQL connection parameters
    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Value("${spring.datasource.username}")
    private String databaseUsername;

    @Value("${spring.datasource.password}")
    private String databasePassword;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    // HikariCP pool settings
    @Value("${spring.datasource.hikari.maximum-pool-size:20}")
    private int maximumPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:5}")
    private int minimumIdle;

    @Value("${spring.datasource.hikari.idle-timeout:300000}")
    private long idleTimeout;

    @Value("${spring.datasource.hikari.connection-timeout:20000}")
    private long connectionTimeout;

    @Value("${spring.datasource.hikari.leak-detection-threshold:60000}")
    private long leakDetectionThreshold;

    // JPA/Hibernate settings
    @Value("${spring.jpa.hibernate.ddl-auto:update}")
    private String ddlAuto;

    @Value("${spring.jpa.show-sql:false}")
    private boolean showSql;

    /**
     * Московский часовой пояс для всех операций с датами
     */
    private static final String MOSCOW_TIMEZONE = "Europe/Moscow";

    /**
     * Конфигурация основного источника данных с оптимизированным пулом соединений
     *
     * @return настроенный DataSource с HikariCP
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        log.info("Configuring MySQL DataSource with Moscow timezone");

        HikariConfig config = new HikariConfig();

        // Основные параметры подключения
        config.setJdbcUrl(databaseUrl);
        config.setUsername(databaseUsername);
        config.setPassword(databasePassword);
        config.setDriverClassName(driverClassName);

        // Настройки пула соединений для высокочастотной торговли
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumIdle);
        config.setIdleTimeout(idleTimeout);
        config.setConnectionTimeout(connectionTimeout);
        config.setLeakDetectionThreshold(leakDetectionThreshold);

        // Дополнительные настройки производительности
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);
        config.setMaxLifetime(1800000); // 30 минут
        config.setKeepaliveTime(600000); // 10 минут

        // Имя пула для мониторинга
        config.setPoolName("ScalpingBotHikariPool");

        // Настройки MySQL для оптимальной производительности
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");

        // Московский часовой пояс
        config.addDataSourceProperty("serverTimezone", MOSCOW_TIMEZONE);
        config.addDataSourceProperty("useLegacyDatetimeCode", "false");

        // SSL и безопасность
        config.addDataSourceProperty("useSSL", "false");
        config.addDataSourceProperty("allowPublicKeyRetrieval", "true");
        config.addDataSourceProperty("requireSSL", "false");

        HikariDataSource dataSource = new HikariDataSource(config);

        log.info("MySQL DataSource configured successfully:");
        log.info("- Pool size: {} (min: {})", maximumPoolSize, minimumIdle);
        log.info("- Connection timeout: {}ms", connectionTimeout);
        log.info("- Timezone: {}", MOSCOW_TIMEZONE);

        return dataSource;
    }

    /**
     * Конфигурация EntityManagerFactory с настройками Hibernate
     *
     * @param dataSource источник данных
     * @return настроенная фабрика EntityManager
     */
    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        log.info("Configuring JPA EntityManagerFactory");

        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.example.scalpingBot.entity");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setShowSql(showSql);
        vendorAdapter.setGenerateDdl(true);
        vendorAdapter.setDatabasePlatform("org.hibernate.dialect.MySQLDialect");
        em.setJpaVendorAdapter(vendorAdapter);

        // Hibernate properties для оптимизации производительности
        Properties properties = new Properties();

        // Основные настройки
        properties.setProperty("hibernate.hbm2ddl.auto", ddlAuto);
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        properties.setProperty("hibernate.format_sql", "true");
        properties.setProperty("hibernate.use_sql_comments", "true");

        // Московский часовой пояс
        properties.setProperty("hibernate.jdbc.time_zone", MOSCOW_TIMEZONE);

        // Настройки производительности для скальпинга
        properties.setProperty("hibernate.jdbc.batch_size", "25");
        properties.setProperty("hibernate.order_inserts", "true");
        properties.setProperty("hibernate.order_updates", "true");
        properties.setProperty("hibernate.batch_versioned_data", "true");

        // Кеширование второго уровня
        properties.setProperty("hibernate.cache.use_second_level_cache", "true");
        properties.setProperty("hibernate.cache.use_query_cache", "true");
        properties.setProperty("hibernate.cache.region.factory_class",
                "org.hibernate.cache.jcache.JCacheRegionFactory");

        // Статистика для мониторинга
        properties.setProperty("hibernate.generate_statistics", "true");

        // Оптимизация соединений
        properties.setProperty("hibernate.connection.provider_disables_autocommit", "true");
        properties.setProperty("hibernate.connection.autocommit", "false");

        // Настройки для работы с большими данными
        properties.setProperty("hibernate.jdbc.fetch_size", "50");
        properties.setProperty("hibernate.default_batch_fetch_size", "16");

        em.setJpaProperties(properties);

        log.info("JPA EntityManagerFactory configured with:");
        log.info("- DDL mode: {}", ddlAuto);
        log.info("- Show SQL: {}", showSql);
        log.info("- Batch size: 25");
        log.info("- Second level cache: enabled");

        return em;
    }

    /**
     * Конфигурация менеджера транзакций
     *
     * @param entityManagerFactory фабрика EntityManager
     * @return настроенный менеджер транзакций
     */
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        log.info("Configuring JPA Transaction Manager");

        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);

        // Настройки таймаутов для торговых операций
        transactionManager.setDefaultTimeout(30); // 30 секунд максимум
        transactionManager.setRollbackOnCommitFailure(true);

        log.info("Transaction Manager configured with 30s timeout");

        return transactionManager;
    }

    /**
     * Проверка и установка московского часового пояса
     */
    @Bean
    public TimeZone configureTimezone() {
        TimeZone moscowTimeZone = TimeZone.getTimeZone(MOSCOW_TIMEZONE);
        TimeZone.setDefault(moscowTimeZone);

        log.info("System timezone set to: {}", moscowTimeZone.getID());
        log.info("Current time offset: GMT{}",
                moscowTimeZone.getRawOffset() / (1000 * 60 * 60));

        return moscowTimeZone;
    }
}