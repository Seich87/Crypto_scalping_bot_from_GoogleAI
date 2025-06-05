package com.example.scalpingBot.repository;

import com.example.scalpingBot.entity.Trade;
import com.example.scalpingBot.enums.OrderSide;
import com.example.scalpingBot.enums.OrderStatus;
import com.example.scalpingBot.enums.OrderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с торговыми операциями
 *
 * Предоставляет методы для:
 * - Поиска и фильтрации торговых операций
 * - Расчета статистики и метрик производительности
 * - Анализа P&L и эффективности стратегии
 * - Мониторинга активных ордеров
 * - Получения исторических данных для оптимизации
 *
 * Все запросы оптимизированы для скальпинг-стратегии
 * с учетом высокой частоты операций и требований
 * к производительности в реальном времени.
 *
 * @author ScalpingBot Team
 * @version 1.0
 */
@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {

    // === Поиск по основным параметрам ===

    /**
     * Найти торговую операцию по ID ордера на бирже
     */
    Optional<Trade> findByExchangeOrderId(String exchangeOrderId);

    /**
     * Найти торговую операцию по клиентскому ID ордера
     */
    Optional<Trade> findByClientOrderId(String clientOrderId);

    /**
     * Найти все операции по торговой паре
     */
    List<Trade> findByTradingPairOrderByCreatedAtDesc(String tradingPair);

    /**
     * Найти все операции по торговой паре с пагинацией
     */
    Page<Trade> findByTradingPairOrderByCreatedAtDesc(String tradingPair, Pageable pageable);

    /**
     * Найти все операции по позиции
     */
    List<Trade> findByPositionIdOrderByCreatedAtAsc(Long positionId);

    /**
     * Найти все операции со статусом
     */
    List<Trade> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    /**
     * Найти все активные ордера (ожидающие исполнения)
     */
    @Query("SELECT t FROM Trade t WHERE t.status IN ('SUBMITTED', 'PARTIALLY_FILLED') ORDER BY t.createdAt DESC")
    List<Trade> findActiveOrders();

    /**
     * Найти активные ордера по торговой паре
     */
    @Query("SELECT t FROM Trade t WHERE t.tradingPair = :pair AND t.status IN ('SUBMITTED', 'PARTIALLY_FILLED') ORDER BY t.createdAt DESC")
    List<Trade> findActiveOrdersByPair(@Param("pair") String tradingPair);

    // === Поиск по временным диапазонам ===

    /**
     * Найти операции за период
     */
    List<Trade> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Найти операции за сегодня
     */
    @Query("SELECT t FROM Trade t WHERE DATE(t.createdAt) = CURRENT_DATE ORDER BY t.createdAt DESC")
    List<Trade> findTodayTrades();

    /**
     * Найти операции за последние N часов
     */
    @Query("SELECT t FROM Trade t WHERE t.createdAt >= :since ORDER BY t.createdAt DESC")
    List<Trade> findTradesSince(@Param("since") LocalDateTime since);

    /**
     * Найти скальпинг операции за период
     */
    @Query("SELECT t FROM Trade t WHERE t.isScalping = true AND t.createdAt BETWEEN :start AND :end ORDER BY t.createdAt DESC")
    List<Trade> findScalpingTradesBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // === Статистические запросы ===

    /**
     * Подсчитать общее количество операций
     */
    long count();

    /**
     * Подсчитать операции по торговой паре
     */
    long countByTradingPair(String tradingPair);

    /**
     * Подсчитать прибыльные операции
     */
    @Query("SELECT COUNT(t) FROM Trade t WHERE t.realizedPnl > 0")
    long countProfitableTrades();

    /**
     * Подсчитать прибыльные операции по паре
     */
    @Query("SELECT COUNT(t) FROM Trade t WHERE t.tradingPair = :pair AND t.realizedPnl > 0")
    long countProfitableTradesByPair(@Param("pair") String tradingPair);

    /**
     * Подсчитать операции за сегодня
     */
    @Query("SELECT COUNT(t) FROM Trade t WHERE DATE(t.createdAt) = CURRENT_DATE")
    long countTodayTrades();

    /**
     * Подсчитать скальпинг операции
     */
    long countByIsScalpingTrue();

    // === Расчет P&L и метрик ===

    /**
     * Рассчитать общий P&L
     */
    @Query("SELECT COALESCE(SUM(t.realizedPnl), 0) FROM Trade t WHERE t.realizedPnl IS NOT NULL")
    BigDecimal calculateTotalPnl();

    /**
     * Рассчитать P&L по торговой паре
     */
    @Query("SELECT COALESCE(SUM(t.realizedPnl), 0) FROM Trade t WHERE t.tradingPair = :pair AND t.realizedPnl IS NOT NULL")
    BigDecimal calculatePnlByPair(@Param("pair") String tradingPair);

    /**
     * Рассчитать дневной P&L
     */
    @Query("SELECT COALESCE(SUM(t.realizedPnl), 0) FROM Trade t WHERE DATE(t.createdAt) = CURRENT_DATE AND t.realizedPnl IS NOT NULL")
    BigDecimal calculateDailyPnl();

    /**
     * Рассчитать P&L за период
     */
    @Query("SELECT COALESCE(SUM(t.realizedPnl), 0) FROM Trade t WHERE t.createdAt BETWEEN :start AND :end AND t.realizedPnl IS NOT NULL")
    BigDecimal calculatePnlBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Рассчитать средний P&L на операцию
     */
    @Query("SELECT COALESCE(AVG(t.realizedPnl), 0) FROM Trade t WHERE t.realizedPnl IS NOT NULL")
    BigDecimal calculateAveragePnl();

    /**
     * Рассчитать общие комиссии
     */
    @Query("SELECT COALESCE(SUM(t.commission), 0) FROM Trade t WHERE t.commission IS NOT NULL")
    BigDecimal calculateTotalCommissions();

    /**
     * Рассчитать общий объем торгов
     */
    @Query("SELECT COALESCE(SUM(t.totalValue), 0) FROM Trade t WHERE t.totalValue IS NOT NULL")
    BigDecimal calculateTotalTradingVolume();

    // === Анализ производительности ===

    /**
     * Найти самую прибыльную операцию
     */
    @Query("SELECT t FROM Trade t WHERE t.realizedPnl = (SELECT MAX(t2.realizedPnl) FROM Trade t2 WHERE t2.realizedPnl IS NOT NULL)")
    Optional<Trade> findMostProfitableTrade();

    /**
     * Найти самую убыточную операцию
     */
    @Query("SELECT t FROM Trade t WHERE t.realizedPnl = (SELECT MIN(t2.realizedPnl) FROM Trade t2 WHERE t2.realizedPnl IS NOT NULL)")
    Optional<Trade> findMostLosingTrade();

    /**
     * Найти операции с наибольшим объемом
     */
    List<Trade> findTop10ByTotalValueOrderByTotalValueDesc();

    /**
     * Найти последние исполненные операции
     */
    List<Trade> findTop20ByStatusAndExecutedAtIsNotNullOrderByExecutedAtDesc(OrderStatus status);

    /**
     * Получить статистику по времени удержания позиций
     */
    @Query("SELECT AVG(t.holdingTimeMinutes), MIN(t.holdingTimeMinutes), MAX(t.holdingTimeMinutes) " +
            "FROM Trade t WHERE t.holdingTimeMinutes IS NOT NULL")
    Object[] getHoldingTimeStatistics();

    // === Анализ по торговым парам ===

    /**
     * Получить топ торговых пар по прибыльности
     */
    @Query("SELECT t.tradingPair, SUM(t.realizedPnl) as totalPnl, COUNT(t) as tradesCount " +
            "FROM Trade t WHERE t.realizedPnl IS NOT NULL " +
            "GROUP BY t.tradingPair ORDER BY totalPnl DESC")
    List<Object[]> getTopPairsByProfitability();

    /**
     * Получить статистику по торговым парам
     */
    @Query("SELECT t.tradingPair, " +
            "COUNT(t) as totalTrades, " +
            "SUM(CASE WHEN t.realizedPnl > 0 THEN 1 ELSE 0 END) as profitableTrades, " +
            "SUM(t.realizedPnl) as totalPnl, " +
            "AVG(t.realizedPnl) as avgPnl " +
            "FROM Trade t WHERE t.realizedPnl IS NOT NULL " +
            "GROUP BY t.tradingPair ORDER BY totalPnl DESC")
    List<Object[]> getPairStatistics();

    /**
     * Получить активность по торговым парам за сегодня
     */
    @Query("SELECT t.tradingPair, COUNT(t) as tradesCount, SUM(t.totalValue) as volume " +
            "FROM Trade t WHERE DATE(t.createdAt) = CURRENT_DATE " +
            "GROUP BY t.tradingPair ORDER BY tradesCount DESC")
    List<Object[]> getTodayActivityByPair();

    // === Анализ по стратегиям ===

    /**
     * Получить статистику по стратегиям
     */
    @Query("SELECT t.strategyName, " +
            "COUNT(t) as totalTrades, " +
            "SUM(CASE WHEN t.realizedPnl > 0 THEN 1 ELSE 0 END) as profitableTrades, " +
            "SUM(t.realizedPnl) as totalPnl, " +
            "AVG(t.realizedPnl) as avgPnl " +
            "FROM Trade t WHERE t.strategyName IS NOT NULL AND t.realizedPnl IS NOT NULL " +
            "GROUP BY t.strategyName ORDER BY totalPnl DESC")
    List<Object[]> getStrategyStatistics();

    // === Мониторинг и контроль ===

    /**
     * Найти просроченные ордера (старше N минут)
     */
    @Query("SELECT t FROM Trade t WHERE t.status IN ('SUBMITTED', 'PARTIALLY_FILLED') " +
            "AND t.createdAt < :threshold ORDER BY t.createdAt ASC")
    List<Trade> findExpiredOrders(@Param("threshold") LocalDateTime threshold);

    /**
     * Найти операции с подозрительно высоким slippage
     */
    @Query("SELECT t FROM Trade t WHERE t.slippagePercent > :maxSlippage ORDER BY t.slippagePercent DESC")
    List<Trade> findHighSlippageTrades(@Param("maxSlippage") BigDecimal maxSlippage);

    /**
     * Найти операции с высокими потерями
     */
    @Query("SELECT t FROM Trade t WHERE t.realizedPnl < :minLoss ORDER BY t.realizedPnl ASC")
    List<Trade> findHighLossTrades(@Param("minLoss") BigDecimal minLoss);

    /**
     * Найти неполностью исполненные ордера
     */
    @Query("SELECT t FROM Trade t WHERE t.status = 'PARTIALLY_FILLED' " +
            "AND t.executedQuantity < t.quantity ORDER BY t.createdAt ASC")
    List<Trade> findPartiallyFilledOrders();

    // === Технический анализ ===

    /**
     * Найти операции по техническим сигналам
     */
    @Query("SELECT t FROM Trade t WHERE t.rsiValue BETWEEN :minRsi AND :maxRsi " +
            "ORDER BY t.createdAt DESC")
    List<Trade> findTradesByRsiRange(@Param("minRsi") BigDecimal minRsi, @Param("maxRsi") BigDecimal maxRsi);

    /**
     * Найти операции с сильными сигналами
     */
    @Query("SELECT t FROM Trade t WHERE ABS(t.signalStrength) > :minStrength " +
            "ORDER BY ABS(t.signalStrength) DESC")
    List<Trade> findTradesWithStrongSignals(@Param("minStrength") BigDecimal minStrength);

    // === Корреляционный анализ ===

    /**
     * Найти одновременные операции (для анализа корреляции)
     */
    @Query("SELECT t FROM Trade t WHERE t.createdAt BETWEEN :start AND :end " +
            "AND t.tradingPair IN :pairs ORDER BY t.createdAt ASC")
    List<Trade> findSimultaneousTradesInPairs(@Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end,
                                              @Param("pairs") List<String> pairs);

    // === Очистка данных ===

    /**
     * Удалить старые операции (для очистки истории)
     */
    @Query("DELETE FROM Trade t WHERE t.createdAt < :threshold")
    void deleteTradesOlderThan(@Param("threshold") LocalDateTime threshold);

    /**
     * Найти операции для архивации
     */
    @Query("SELECT t FROM Trade t WHERE t.createdAt < :threshold " +
            "AND t.status IN ('FILLED', 'CANCELLED', 'REJECTED', 'FAILED') " +
            "ORDER BY t.createdAt ASC")
    List<Trade> findTradesForArchiving(@Param("threshold") LocalDateTime threshold);

    // === Быстрые проверки статуса ===

    /**
     * Проверить наличие активных ордеров
     */
    @Query("SELECT COUNT(t) > 0 FROM Trade t WHERE t.status IN ('SUBMITTED', 'PARTIALLY_FILLED')")
    boolean hasActiveOrders();

    /**
     * Проверить наличие активных ордеров по паре
     */
    @Query("SELECT COUNT(t) > 0 FROM Trade t WHERE t.tradingPair = :pair " +
            "AND t.status IN ('SUBMITTED', 'PARTIALLY_FILLED')")
    boolean hasActiveOrdersForPair(@Param("pair") String tradingPair);

    /**
     * Получить количество операций за последний час
     */
    @Query("SELECT COUNT(t) FROM Trade t WHERE t.createdAt >= :oneHourAgo")
    long countTradesInLastHour(@Param("oneHourAgo") LocalDateTime oneHourAgo);

    // === Расчет производительности ===

    /**
     * Рассчитать коэффициент выигрышных сделок (win rate)
     */
    @Query("SELECT " +
            "CAST(SUM(CASE WHEN t.realizedPnl > 0 THEN 1 ELSE 0 END) AS DOUBLE) / COUNT(t) * 100 " +
            "FROM Trade t WHERE t.realizedPnl IS NOT NULL")
    Double calculateWinRate();

    /**
     * Рассчитать коэффициент выигрышных сделок по паре
     */
    @Query("SELECT " +
            "CAST(SUM(CASE WHEN t.realizedPnl > 0 THEN 1 ELSE 0 END) AS DOUBLE) / COUNT(t) * 100 " +
            "FROM Trade t WHERE t.tradingPair = :pair AND t.realizedPnl IS NOT NULL")
    Double calculateWinRateByPair(@Param("pair") String tradingPair);

    /**
     * Рассчитать средний profit factor
     */
    @Query("SELECT " +
            "ABS(SUM(CASE WHEN t.realizedPnl > 0 THEN t.realizedPnl ELSE 0 END)) / " +
            "ABS(SUM(CASE WHEN t.realizedPnl < 0 THEN t.realizedPnl ELSE 0 END)) " +
            "FROM Trade t WHERE t.realizedPnl IS NOT NULL")
    Double calculateProfitFactor();
}