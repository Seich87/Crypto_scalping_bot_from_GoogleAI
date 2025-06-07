-- V1__Create_initial_schema.sql
-- Начальная схема базы данных для скальпинг-бота

-- ===================================================================
-- ТАБЛИЦА ТОРГОВЫХ ПАР
-- ===================================================================
CREATE TABLE trading_pairs (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               symbol VARCHAR(20) NOT NULL UNIQUE COMMENT 'Символ торговой пары (например, BTCUSDT)',
                               base_asset VARCHAR(10) NOT NULL COMMENT 'Базовый актив (например, BTC)',
                               quote_asset VARCHAR(10) NOT NULL COMMENT 'Квотируемый актив (например, USDT)',
                               type ENUM('SPOT', 'FUTURES_PERPETUAL', 'FUTURES_DATED') NOT NULL DEFAULT 'SPOT',
                               is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Активна ли пара для торговли',
                               price_precision INT NOT NULL DEFAULT 8 COMMENT 'Точность цены (знаков после запятой)',
                               quantity_precision INT NOT NULL DEFAULT 8 COMMENT 'Точность количества',
                               min_order_size DECIMAL(19,8) NULL COMMENT 'Минимальный размер ордера в quote_asset',
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Конфигурация торговых пар';

-- ===================================================================
-- ТАБЛИЦА РЫНОЧНЫХ ДАННЫХ
-- ===================================================================
CREATE TABLE market_data (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             trading_pair VARCHAR(20) NOT NULL COMMENT 'Торговая пара',
                             timestamp TIMESTAMP(3) NOT NULL COMMENT 'Временная метка данных',
                             last_price DECIMAL(19,8) NOT NULL COMMENT 'Последняя цена',
                             best_bid_price DECIMAL(19,8) NULL COMMENT 'Лучшая цена покупки',
                             best_ask_price DECIMAL(19,8) NULL COMMENT 'Лучшая цена продажи',
                             volume_24h DECIMAL(24,8) NULL COMMENT 'Объем торгов за 24 часа'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Исторические рыночные данные';

-- ===================================================================
-- ТАБЛИЦА ПОЗИЦИЙ
-- ===================================================================
CREATE TABLE positions (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           trading_pair VARCHAR(20) NOT NULL COMMENT 'Торговая пара',
                           side ENUM('BUY', 'SELL') NOT NULL COMMENT 'Сторона позиции (BUY=long, SELL=short)',
                           quantity DECIMAL(19,8) NOT NULL COMMENT 'Количество актива в позиции',
                           entry_price DECIMAL(19,8) NOT NULL COMMENT 'Средняя цена входа',
                           stop_loss_price DECIMAL(19,8) NULL COMMENT 'Цена стоп-лосса',
                           take_profit_price DECIMAL(19,8) NULL COMMENT 'Цена тейк-профита',
                           trailing_stop_percentage DECIMAL(10,4) NULL COMMENT 'Процент трейлинг-стопа',
                           is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Активна ли позиция',
                           open_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Время открытия',
                           close_timestamp TIMESTAMP NULL COMMENT 'Время закрытия',
                           pnl DECIMAL(19,8) NULL COMMENT 'Прибыль/убыток при закрытии'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Торговые позиции';

-- ===================================================================
-- ТАБЛИЦА СДЕЛОК
-- ===================================================================
CREATE TABLE trades (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        exchange_trade_id VARCHAR(50) NOT NULL UNIQUE COMMENT 'ID сделки на бирже',
                        trading_pair VARCHAR(20) NOT NULL COMMENT 'Торговая пара',
                        status ENUM('NEW', 'FILLED', 'PARTIALLY_FILLED', 'CANCELED', 'PENDING_CANCEL', 'REJECTED', 'EXPIRED') NOT NULL,
                        side ENUM('BUY', 'SELL') NOT NULL COMMENT 'Сторона сделки',
                        type ENUM('MARKET', 'LIMIT', 'STOP_LOSS', 'TAKE_PROFIT', 'STOP_LOSS_LIMIT', 'TAKE_PROFIT_LIMIT') NOT NULL,
                        price DECIMAL(19,8) NOT NULL COMMENT 'Цена исполнения',
                        quantity DECIMAL(19,8) NOT NULL COMMENT 'Количество',
                        commission DECIMAL(19,8) NULL COMMENT 'Комиссия',
                        commission_asset VARCHAR(10) NULL COMMENT 'Актив комиссии',
                        execution_timestamp TIMESTAMP NOT NULL COMMENT 'Время исполнения на бирже',
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Время создания записи'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Совершенные сделки';

-- ===================================================================
-- ТАБЛИЦА СОБЫТИЙ РИСКА
-- ===================================================================
CREATE TABLE risk_events (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             position_id BIGINT NULL COMMENT 'Связанная позиция',
                             trading_pair VARCHAR(20) NOT NULL COMMENT 'Торговая пара',
                             event_type VARCHAR(50) NOT NULL COMMENT 'Тип события (STOP_LOSS_TRIGGERED, LIQUIDATION_WARNING и т.д.)',
                             trigger_price DECIMAL(19,8) NULL COMMENT 'Цена срабатывания',
                             message VARCHAR(512) NULL COMMENT 'Подробное описание события',
                             event_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Время события'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='События управления рисками';

-- ===================================================================
-- ТАБЛИЦА КОНФИГУРАЦИЙ СТРАТЕГИЙ
-- ===================================================================
CREATE TABLE strategy_configs (
                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  trading_pair VARCHAR(20) NOT NULL UNIQUE COMMENT 'Торговая пара',
                                  strategy_name VARCHAR(100) NOT NULL COMMENT 'Название стратегии',
                                  is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Активна ли стратегия',
                                  parameters JSON NULL COMMENT 'Параметры стратегии в JSON формате'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Конфигурации торговых стратегий';

-- ===================================================================
-- СОЗДАНИЕ ИНДЕКСОВ ДЛЯ ОПТИМИЗАЦИИ ЗАПРОСОВ
-- ===================================================================

-- Индексы для trading_pairs
CREATE INDEX idx_trading_pairs_active_type ON trading_pairs(is_active, type);
CREATE INDEX idx_trading_pairs_symbol ON trading_pairs(symbol);

-- Индексы для market_data (критически важны для производительности)
CREATE INDEX idx_market_data_pair_timestamp ON market_data(trading_pair, timestamp);
CREATE INDEX idx_market_data_timestamp ON market_data(timestamp);
-- Составной индекс для частых запросов по паре и временному диапазону
CREATE INDEX idx_market_data_pair_time_range ON market_data(trading_pair, timestamp, last_price);

-- Индексы для positions
CREATE INDEX idx_positions_active_pair ON positions(is_active, trading_pair);
CREATE INDEX idx_positions_pair_status ON positions(trading_pair, is_active);
CREATE INDEX idx_positions_timestamps ON positions(open_timestamp, close_timestamp);
-- Для анализа производительности
CREATE INDEX idx_positions_closed_by_close_time ON positions(is_active, close_timestamp);

-- Индексы для trades
CREATE INDEX idx_trades_pair_timestamp ON trades(trading_pair, execution_timestamp);
CREATE INDEX idx_trades_exchange_id ON trades(exchange_trade_id);
CREATE INDEX idx_trades_timestamp ON trades(execution_timestamp);
CREATE INDEX idx_trades_pair_status ON trades(trading_pair, status);

-- Индексы для risk_events
CREATE INDEX idx_risk_events_pair_timestamp ON risk_events(trading_pair, event_timestamp);
CREATE INDEX idx_risk_events_position ON risk_events(position_id);
CREATE INDEX idx_risk_events_type ON risk_events(event_type);
CREATE INDEX idx_risk_events_timestamp ON risk_events(event_timestamp);

-- Индексы для strategy_configs
CREATE INDEX idx_strategy_configs_active ON strategy_configs(is_active);
CREATE INDEX idx_strategy_configs_strategy ON strategy_configs(strategy_name);

-- ===================================================================
-- ВНЕШНИЕ КЛЮЧИ
-- ===================================================================

-- Связь risk_events с positions
ALTER TABLE risk_events
    ADD CONSTRAINT fk_risk_events_position
        FOREIGN KEY (position_id) REFERENCES positions(id)
            ON DELETE SET NULL ON UPDATE CASCADE;

-- ===================================================================
-- ИНИЦИАЛИЗАЦИЯ ДАННЫХ
-- ===================================================================

-- Добавляем основные торговые пары
INSERT INTO trading_pairs (symbol, base_asset, quote_asset, type, is_active, price_precision, quantity_precision, min_order_size) VALUES
                                                                                                                                      ('BTCUSDT', 'BTC', 'USDT', 'SPOT', TRUE, 2, 6, 10.00),
                                                                                                                                      ('ETHUSDT', 'ETH', 'USDT', 'SPOT', TRUE, 2, 5, 10.00),
                                                                                                                                      ('BNBUSDT', 'BNB', 'USDT', 'SPOT', TRUE, 2, 3, 10.00),
                                                                                                                                      ('ADAUSDT', 'ADA', 'USDT', 'SPOT', TRUE, 4, 1, 10.00),
                                                                                                                                      ('DOTUSDT', 'DOT', 'USDT', 'SPOT', TRUE, 3, 2, 10.00);

-- ===================================================================
-- КОММЕНТАРИИ К ТАБЛИЦАМ
-- ===================================================================

-- Дополнительные комментарии для clarity
ALTER TABLE market_data COMMENT = 'Снимки рыночных данных для технического анализа. Партиционирование по времени рекомендуется для больших объемов';
ALTER TABLE positions COMMENT = 'Торговые позиции с полным жизненным циклом и параметрами риск-менеджмента';
ALTER TABLE trades COMMENT = 'Журнал всех совершенных сделок для аудита и анализа';
ALTER TABLE risk_events COMMENT = 'Лог событий системы риск-менеджмента для мониторинга и анализа';
ALTER TABLE strategy_configs COMMENT = 'Гибкая конфигурация торговых стратегий с JSON параметрами';