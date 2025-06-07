-- V2__Performance_optimization_and_partitioning.sql
-- Оптимизация производительности и подготовка к партиционированию

-- ===================================================================
-- ДОПОЛНИТЕЛЬНЫЕ ИНДЕКСЫ ДЛЯ КРИТИЧЕСКИХ ЗАПРОСОВ
-- ===================================================================

-- Составные индексы для технического анализа (самые частые запросы)
CREATE INDEX idx_market_data_analysis ON market_data(trading_pair, timestamp DESC, last_price);
CREATE INDEX idx_market_data_recent ON market_data(timestamp DESC, trading_pair, last_price)
    WHERE timestamp >= DATE_SUB(NOW(), INTERVAL 7 DAY);

-- Индексы для быстрого поиска активных позиций по цене
CREATE INDEX idx_positions_active_prices ON positions(is_active, trading_pair, entry_price, stop_loss_price)
    WHERE is_active = 1;

-- Индексы для риск-мониторинга
CREATE INDEX idx_positions_stop_loss ON positions(is_active, stop_loss_price, trading_pair)
    WHERE is_active = 1 AND stop_loss_price IS NOT NULL;

CREATE INDEX idx_positions_take_profit ON positions(is_active, take_profit_price, trading_pair)
    WHERE is_active = 1 AND take_profit_price IS NOT NULL;

CREATE INDEX idx_positions_trailing_stop ON positions(is_active, trailing_stop_percentage, trading_pair)
    WHERE is_active = 1 AND trailing_stop_percentage IS NOT NULL;

-- Индексы для анализа производительности стратегий
CREATE INDEX idx_positions_pnl_analysis ON positions(is_active, close_timestamp, pnl, trading_pair)
    WHERE is_active = 0 AND pnl IS NOT NULL;

-- Индексы для мониторинга сделок в реальном времени
CREATE INDEX idx_trades_recent ON trades(execution_timestamp DESC, trading_pair, status);
CREATE INDEX idx_trades_status_pair ON trades(status, trading_pair, execution_timestamp DESC);

-- Индексы для поиска дублей сделок
CREATE INDEX idx_trades_dedup ON trades(exchange_trade_id, trading_pair, execution_timestamp);

-- Индексы для событий риска
CREATE INDEX idx_risk_events_monitoring ON risk_events(event_timestamp DESC, event_type, trading_pair);
CREATE INDEX idx_risk_events_position_time ON risk_events(position_id, event_timestamp DESC);

-- ===================================================================
-- ОПТИМИЗАЦИЯ КОНФИГУРАЦИИ ТАБЛИЦ
-- ===================================================================

-- Оптимизация market_data для быстрых вставок и селектов
ALTER TABLE market_data
    ENGINE=InnoDB
        ROW_FORMAT=COMPRESSED
        KEY_BLOCK_SIZE=8
        COMMENT='Optimized for high-frequency inserts and time-based selects';

-- Оптимизация trades для быстрого поиска
ALTER TABLE trades
    ENGINE=InnoDB
        ROW_FORMAT=DYNAMIC
        COMMENT='Optimized for fast lookups and audit queries';

-- ===================================================================
-- ПРЕДСТАВЛЕНИЯ ДЛЯ ЧАСТО ИСПОЛЬЗУЕМЫХ ЗАПРОСОВ
-- ===================================================================

-- Представление для активных позиций с текущими метриками
CREATE VIEW v_active_positions AS
SELECT
    p.id,
    p.trading_pair,
    p.side,
    p.quantity,
    p.entry_price,
    p.stop_loss_price,
    p.take_profit_price,
    p.trailing_stop_percentage,
    p.open_timestamp,
    TIMESTAMPDIFF(MINUTE, p.open_timestamp, NOW()) as age_minutes,
    CASE
        WHEN p.stop_loss_price IS NOT NULL
            THEN ABS(((p.stop_loss_price - p.entry_price) / p.entry_price) * 100)
        ELSE NULL
        END as stop_loss_distance_percent,
    CASE
        WHEN p.take_profit_price IS NOT NULL
            THEN ABS(((p.take_profit_price - p.entry_price) / p.entry_price) * 100)
        ELSE NULL
        END as take_profit_distance_percent
FROM positions p
WHERE p.is_active = 1;

-- Представление для последних рыночных данных по парам
CREATE VIEW v_latest_market_data AS
SELECT
    md1.trading_pair,
    md1.last_price,
    md1.best_bid_price,
    md1.best_ask_price,
    md1.volume_24h,
    md1.timestamp,
    TIMESTAMPDIFF(SECOND, md1.timestamp, NOW()) as data_age_seconds
FROM market_data md1
         INNER JOIN (
    SELECT trading_pair, MAX(timestamp) as max_timestamp
    FROM market_data
    WHERE timestamp >= DATE_SUB(NOW(), INTERVAL 1 HOUR)
    GROUP BY trading_pair
) md2 ON md1.trading_pair = md2.trading_pair AND md1.timestamp = md2.max_timestamp;

-- Представление для статистики торговли по парам
CREATE VIEW v_trading_stats AS
SELECT
    trading_pair,
    COUNT(*) as total_trades,
    SUM(CASE WHEN status = 'FILLED' THEN 1 ELSE 0 END) as filled_trades,
    AVG(CASE WHEN status = 'FILLED' THEN price ELSE NULL END) as avg_fill_price,
    SUM(CASE WHEN status = 'FILLED' THEN quantity ELSE 0 END) as total_volume,
    MIN(execution_timestamp) as first_trade,
    MAX(execution_timestamp) as last_trade
FROM trades
WHERE execution_timestamp >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
GROUP BY trading_pair;

-- Представление для мониторинга производительности позиций
CREATE VIEW v_position_performance AS
SELECT
    trading_pair,
    COUNT(*) as total_positions,
    SUM(CASE WHEN pnl > 0 THEN 1 ELSE 0 END) as winning_positions,
    SUM(CASE WHEN pnl <= 0 THEN 1 ELSE 0 END) as losing_positions,
    ROUND((SUM(CASE WHEN pnl > 0 THEN 1 ELSE 0 END) / COUNT(*)) * 100, 2) as win_rate_percent,
    SUM(pnl) as total_pnl,
    AVG(pnl) as avg_pnl,
    MAX(pnl) as best_trade,
    MIN(pnl) as worst_trade,
    AVG(TIMESTAMPDIFF(MINUTE, open_timestamp, close_timestamp)) as avg_duration_minutes
FROM positions
WHERE is_active = 0 AND pnl IS NOT NULL AND close_timestamp >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY trading_pair;

-- ===================================================================
-- ПРОЦЕДУРЫ ДЛЯ ОБСЛУЖИВАНИЯ БД
-- ===================================================================

-- Процедура для очистки старых рыночных данных
DELIMITER //
CREATE PROCEDURE CleanupOldMarketData(IN days_to_keep INT)
BEGIN
    DECLARE affected_rows INT DEFAULT 0;

    DELETE FROM market_data
    WHERE timestamp < DATE_SUB(NOW(), INTERVAL days_to_keep DAY);

    SET affected_rows = ROW_COUNT();

    INSERT INTO risk_events (trading_pair, event_type, message, event_timestamp)
    VALUES ('SYSTEM', 'DATA_CLEANUP',
            CONCAT('Cleaned up ', affected_rows, ' old market data records older than ', days_to_keep, ' days'),
            NOW());

    SELECT CONCAT('Cleanup completed. Removed ', affected_rows, ' records.') as result;
END //
DELIMITER ;

-- Процедура для анализа индексов
DELIMITER //
CREATE PROCEDURE AnalyzeTablePerformance()
BEGIN
    SELECT
        TABLE_NAME,
        TABLE_ROWS,
        ROUND(DATA_LENGTH / 1024 / 1024, 2) as DATA_SIZE_MB,
        ROUND(INDEX_LENGTH / 1024 / 1024, 2) as INDEX_SIZE_MB,
        ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) as TOTAL_SIZE_MB
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
    ORDER BY (DATA_LENGTH + INDEX_LENGTH) DESC;
END //
DELIMITER ;

-- Процедура для мониторинга неиспользуемых индексов
DELIMITER //
CREATE PROCEDURE CheckUnusedIndexes()
BEGIN
    SELECT
        s.TABLE_SCHEMA,
        s.TABLE_NAME,
        s.INDEX_NAME,
        s.CARDINALITY
    FROM information_schema.STATISTICS s
             LEFT JOIN performance_schema.table_io_waits_summary_by_index_usage u
                       ON s.TABLE_SCHEMA = u.OBJECT_SCHEMA
                           AND s.TABLE_NAME = u.OBJECT_NAME
                           AND s.INDEX_NAME = u.INDEX_NAME
    WHERE s.TABLE_SCHEMA = DATABASE()
      AND s.INDEX_NAME != 'PRIMARY'
      AND (u.COUNT_READ IS NULL OR u.COUNT_READ = 0)
    ORDER BY s.TABLE_NAME, s.INDEX_NAME;
END //
DELIMITER ;

-- ===================================================================
-- ТРИГГЕРЫ ДЛЯ АВТОМАТИЧЕСКОГО ОБСЛУЖИВАНИЯ
-- ===================================================================

-- Триггер для автоматического логирования изменений в позициях
DELIMITER //
CREATE TRIGGER tr_position_changes
    AFTER UPDATE ON positions
    FOR EACH ROW
BEGIN
    IF OLD.stop_loss_price != NEW.stop_loss_price OR
       OLD.take_profit_price != NEW.take_profit_price OR
       OLD.is_active != NEW.is_active THEN

        INSERT INTO risk_events (position_id, trading_pair, event_type, message, event_timestamp)
        VALUES (NEW.id, NEW.trading_pair, 'POSITION_UPDATED',
                CONCAT('Position updated: SL=', COALESCE(NEW.stop_loss_price, 'NULL'),
                       ', TP=', COALESCE(NEW.take_profit_price, 'NULL'),
                       ', Active=', NEW.is_active),
                NOW());
    END IF;
END //
DELIMITER ;

-- Триггер для валидации рыночных данных
DELIMITER //
CREATE TRIGGER tr_market_data_validation
    BEFORE INSERT ON market_data
    FOR EACH ROW
BEGIN
    IF NEW.last_price <= 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid price: must be positive';
    END IF;

    IF NEW.timestamp > NOW() + INTERVAL 1 HOUR THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid timestamp: too far in the future';
    END IF;
END //
DELIMITER ;

-- ===================================================================
-- КОНФИГУРАЦИЯ ДЛЯ ПРОИЗВОДИТЕЛЬНОСТИ
-- ===================================================================

-- Настройки для оптимизации InnoDB
SET GLOBAL innodb_buffer_pool_size = 1073741824; -- 1GB, adjust based on available RAM
SET GLOBAL innodb_log_file_size = 268435456; -- 256MB
SET GLOBAL innodb_flush_log_at_trx_commit = 2; -- Better performance for non-critical data
SET GLOBAL innodb_file_per_table = 1;

-- Настройки для быстрых селектов
SET GLOBAL query_cache_type = 1;
SET GLOBAL query_cache_size = 67108864; -- 64MB

-- ===================================================================
-- ПОДГОТОВКА К ПАРТИЦИОНИРОВАНИЮ (MARKET_DATA)
-- ===================================================================

-- Создание партиционированной таблицы market_data_partitioned для будущего использования
CREATE TABLE market_data_partitioned (
                                         id BIGINT AUTO_INCREMENT,
                                         trading_pair VARCHAR(20) NOT NULL,
                                         timestamp TIMESTAMP(3) NOT NULL,
                                         last_price DECIMAL(19,8) NOT NULL,
                                         best_bid_price DECIMAL(19,8) NULL,
                                         best_ask_price DECIMAL(19,8) NULL,
                                         volume_24h DECIMAL(24,8) NULL,

                                         PRIMARY KEY (id, timestamp),
                                         INDEX idx_pair_time (trading_pair, timestamp),
                                         INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB
    PARTITION BY RANGE (TO_DAYS(timestamp)) (
        PARTITION p_old VALUES LESS THAN (TO_DAYS('2024-01-01')),
        PARTITION p_2024_q1 VALUES LESS THAN (TO_DAYS('2024-04-01')),
        PARTITION p_2024_q2 VALUES LESS THAN (TO_DAYS('2024-07-01')),
        PARTITION p_2024_q3 VALUES LESS THAN (TO_DAYS('2024-10-01')),
        PARTITION p_2024_q4 VALUES LESS THAN (TO_DAYS('2025-01-01')),
        PARTITION p_2025_q1 VALUES LESS THAN (TO_DAYS('2025-04-01')),
        PARTITION p_2025_q2 VALUES LESS THAN (TO_DAYS('2025-07-01')),
        PARTITION p_future VALUES LESS THAN MAXVALUE
        );

-- ===================================================================
-- СОБЫТИЕ ДЛЯ АВТОМАТИЧЕСКОЙ ОЧИСТКИ ДАННЫХ
-- ===================================================================

-- Включаем scheduler для автоматических задач
SET GLOBAL event_scheduler = ON;

-- Автоматическая очистка старых данных каждую ночь
CREATE EVENT ev_cleanup_old_data
    ON SCHEDULE EVERY 1 DAY
        STARTS DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 1 DAY), INTERVAL 2 HOUR) -- 2 AM next day
    DO
    BEGIN
        -- Удаляем market_data старше 30 дней
        DELETE FROM market_data WHERE timestamp < DATE_SUB(NOW(), INTERVAL 30 DAY);

        -- Удаляем risk_events старше 90 дней
        DELETE FROM risk_events WHERE event_timestamp < DATE_SUB(NOW(), INTERVAL 90 DAY);

        -- Оптимизируем таблицы
        OPTIMIZE TABLE market_data, trades, positions, risk_events;

        -- Логируем выполнение
        INSERT INTO risk_events (trading_pair, event_type, message, event_timestamp)
        VALUES ('SYSTEM', 'MAINTENANCE', 'Automated cleanup and optimization completed', NOW());
    END;

-- ===================================================================
-- КОММЕНТАРИИ И ДОКУМЕНТАЦИЯ
-- ===================================================================

-- Добавляем комментарии к новым индексам
ALTER TABLE market_data ADD INDEX idx_market_data_analysis (trading_pair, timestamp DESC, last_price)
    COMMENT 'Critical index for technical analysis queries';

-- Обновляем статистику таблиц
ANALYZE TABLE market_data, positions, trades, risk_events, strategy_configs, trading_pairs;