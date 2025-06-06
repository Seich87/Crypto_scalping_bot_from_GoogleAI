package com.example.scalpingBot.repository;

import com.example.scalpingBot.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA репозиторий для сущности Trade.
 * Предоставляет методы для выполнения CRUD-операций и кастомных запросов к таблице 'trades'.
 */
@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {

    /**
     * Находит сделку по её уникальному идентификатору на бирже.
     * Используется для предотвращения дублирования записей о сделках.
     *
     * @param exchangeTradeId Уникальный ID сделки на бирже.
     * @return Optional, содержащий сделку, если она найдена.
     */
    Optional<Trade> findByExchangeTradeId(String exchangeTradeId);

    /**
     * Находит все сделки для указанной торговой пары, отсортированные по времени исполнения.
     *
     * @param tradingPair Символ торговой пары (например, "BTCUSDT").
     * @return Список сделок.
     */
    List<Trade> findAllByTradingPairOrderByExecutionTimestampDesc(String tradingPair);

    /**
     * Находит все сделки, исполненные в заданном временном интервале.
     *
     * @param start Начало временного интервала.
     * @param end   Конец временного интервала.
     * @return Список сделок.
     */
    List<Trade> findAllByExecutionTimestampBetween(LocalDateTime start, LocalDateTime end);
}