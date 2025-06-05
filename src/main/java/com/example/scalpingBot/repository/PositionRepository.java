package com.example.scalpingBot.repository;

import com.example.scalpingBot.entity.Position;
import com.example.scalpingBot.enums.OrderSide;
import com.example.scalpingBot.enums.RiskLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с торговыми позициями
 *
 * Предоставляет методы для:
 * - Управления активными позициями в реальном времени
 * - Контроля лимитов риск-менеджмента (10 позиций макс)
 * - Мониторинга времени удержания (1 час макс для скальпинга)
 * - Расчета экспозиции портфеля и корреляций
 * - Поиска позиций по различным критериям
 * - Автоматического закрытия просроченных позиций
 *
 * Все запросы оптимизированы для работы скальпинг-стратегии
 * с упором на быстродействие и контроль рисков.
 *
 * @author ScalpingBot Team
 * @version 1.0
 */
@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {

    // === Управление активными позициями ===

    /**
     * Найти все активные позиции
     */
    List<Position> findByIsActiveTrueOrderByOpenedAtDesc();

    /**
     * Найти активные позиции с пагинацией
     */
    Page<Position> findByIsActiveTrueOrderByOpenedAtDesc(Pageable pageable);

    /**
     * Найти активную позицию по торговой паре
     */
    Optional<Position> findByTradingPairAndIsActiveTrue(String tradingPair);

    /**
     * Найти все позиции по торговой паре (включая закрытые)
     */
    List<Position> findByTradingPairOrderByOpenedAtDesc(String tradingPair);

    /**
     * Найти позиции по направлению (long/short)
     */
    List<Position> findBySideAndIsActiveTrueOrderByOpenedAtDesc(OrderSide side);

    /**
     * Найти позиции по статусу
     */
    List<Position> findByStatusOrderByOpenedAtDesc(Position.PositionStatus status);

    // === Контроль лимитов риск-менеджмента ===

    /**
     * Подсчитать количество активных позиций
     */
    @Query("SELECT COUNT(p) FROM Position p WHERE p.isActive = true")
    long countActivePositions();

    /**
     * Подсчитать активные позиции по бирже
     */
    @Query("SELECT COUNT(p) FROM Position p WHERE p.isActive = true AND p.exchangeName = :exchange")
    long countActivePositionsByExchange(@Param("exchange") String exchangeName);

    /**
     * Проверить, не превышен ли лимит позиций
     */
    @Query("SELECT COUNT(p) < :maxPositions FROM Position p WHERE p.isActive = true")
    boolean canOpenNewPosition(@Param("maxPositions") int maxPositions);

    /**
     * Рассчитать общую экспозицию портфеля
     */
    @Query("SELECT COALESCE(SUM(p.entryValue), 0) FROM Position p WHERE p.isActive = true")
    BigDecimal calculateTotalExposure();

    /**
     * Рассчитать экспозицию по направлению
     */
    @Query("SELECT COALESCE(SUM(p.entryValue), 0) FROM Position p WHERE p.isActive = true AND p.side = :side")
    BigDecimal calculateExposureBySide(@Param("side") OrderSide side);

    /**
     * Найти позиции с высокой экспозицией (>N% от капитала)
     */
    @Query("SELECT p FROM Position p WHERE p.isActive = true AND p.portfolioPercent > :maxPercent ORDER BY p.portfolioPercent DESC")
    List<Position> findHighExposurePositions(@Param("maxPercent") BigDecimal maxPercent);

    // === Мониторинг времени удержания ===

    /**
     * Найти просроченные позиции (превысили максимальное время)
     */
    @Query("SELECT p FROM Position p WHERE p.isActive = true AND p.forceCloseAt < :now ORDER BY p.forceCloseAt ASC")
    List<Position> findExpiredPositions(@Param("now") LocalDateTime now);

    /**
     * Найти позиции, которые скоро истекут (в течение N минут)
     */
    @Query("SELECT p FROM Position p WHERE p.isActive = true " +
            "AND p.forceCloseAt BETWEEN :now AND :threshold ORDER BY p.forceCloseAt ASC")
    List<Position> findPositionsExpiringSoon(@Param("now") LocalDateTime now,
                                             @Param("threshold") LocalDateTime threshold);

    /**
     * Найти позиции старше N минут
     */
    @Query("SELECT p FROM Position p WHERE p.isActive = true AND p.openedAt < :threshold ORDER BY p.openedAt ASC")
    List<Position> findPositionsOlderThan(@Param("threshold") LocalDateTime threshold);

    /**
     * Рассчитать среднее время удержания активных позиций
     */
    @Query("SELECT AVG(FUNCTION('TIMESTAMPDIFF', MINUTE, p.openedAt, CURRENT_TIMESTAMP)) FROM Position p WHERE p.isActive = true")
    Double calculateAverageHoldingTimeMinutes();

    // === Анализ P&L и производительности ===

    /**
     * Рассчитать общий нереализованный P&L
     */
    @Query("SELECT COALESCE(SUM(p.unrealizedPnl), 0) FROM Position p WHERE p.isActive = true")
    BigDecimal calculateTotalUnrealizedPnl();

    /**
     * Рассчитать общий реализованный P&L
     */
    @Query("SELECT COALESCE(SUM(p.realizedPnl), 0) FROM Position p WHERE p.realizedPnl IS NOT NULL")
    BigDecimal calculateTotalRealizedPnl();

    /**
     * Рассчитать дневной P&L (реализованный + нереализованный)
     */
    @Query("SELECT COALESCE(SUM(p.realizedPnl), 0) + COALESCE(SUM(CASE WHEN p.isActive = true THEN p.unrealizedPnl ELSE 0 END), 0) " +
            "FROM Position p WHERE DATE(p.openedAt) = CURRENT_DATE")
    BigDecimal calculateDailyPnl();

    /**
     * Найти самые прибыльные позиции
     */
    @Query("SELECT p FROM Position p WHERE p.isActive = false ORDER BY p.realizedPnl DESC")
    List<Position> findMostProfitablePositions(Pageable pageable);

    /**
     * Найти самые убыточные позиции
     */
    @Query("SELECT p FROM Position p WHERE p.isActive = false ORDER BY p.realizedPnl ASC")
    List<Position> findMostLosingPositions(Pageable pageable);

    /**
     * Найти позиции с убытками больше порога
     */
    @Query("SELECT p FROM Position p WHERE p.isActive = true AND p.unrealizedPnl < :threshold ORDER BY p.unrealizedPnl ASC")
    List<Position> findPositionsWithLossesAbove(@Param("threshold") BigDecimal threshold);

    /**
     * Найти прибыльные активные позиции
     */
    @Query("SELECT p FROM Position p WHERE p.isActive = true AND p.unrealizedPnl > 0 ORDER BY p.unrealizedPnl DESC")
    List<Position> findProfitableActivePositions();

    // === Анализ уровней риска ===

    /**
     * Найти позиции по уровню риска
     */
    List<Position> findByRiskLevelAndIsActiveTrueOrderByOpenedAtDesc(RiskLevel riskLevel);

    /**
     * Подсчитать позиции по уровню риска
     */
    @Query("SELECT COUNT(p) FROM Position p WHERE p.isActive = true AND p.riskLevel = :riskLevel")
    long countActivePositionsByRiskLevel(@Param("riskLevel") RiskLevel riskLevel);

    /**
     * Найти позиции с высоким риском
     */
    @Query("SELECT p FROM Position p WHERE p.isActive = true AND p.riskLevel IN ('HIGH', 'VERY_HIGH', 'CRITICAL') ORDER BY p.riskLevel DESC, p.unrealizedPnl ASC")
    List<Position> findHighRiskPositions();

    /**
     * Рассчитать среднее время до изменения уровня риска
     */
    @Query("SELECT p.riskLevel, AVG(FUNCTION('TIMESTAMPDIFF', MINUTE, p.openedAt, CURRENT_TIMESTAMP)) " +
            "FROM Position p WHERE p.isActive = true GROUP BY p.riskLevel")
    List<Object[]> getAverageTimeByRiskLevel();

    // === Контроль стоп-лоссов и тейк-профитов ===

    /**
     * Найти позиции, которые достигли стоп-лосса
     */
    @Query("SELECT p FROM Position p WHERE p.isActive = true AND p.stopLossPrice IS NOT NULL " +
            "AND ((p.side = 'BUY' AND p.currentPrice <= p.stopLossPrice) " +
            "OR (p.side = 'SELL' AND p.currentPrice >= p.stopLossPrice)) " +
            "ORDER BY p.openedAt ASC")
    List<Position> findPositionsAtStopLoss();

    /**
     * Найти позиции, которые достигли тейк-профита
     */
    @Query("SELECT p FROM Position p WHERE p.isActive = true AND p.takeProfitPrice IS NOT NULL " +
            "AND ((p.side = 'BUY' AND p.currentPrice >= p.takeProfitPrice) " +
            "OR (p.side = 'SELL' AND p.currentPrice <= p.takeProfitPrice)) " +
            "ORDER BY p.openedAt ASC")
    List<Position> findPositionsAtTakeProfit();

    /**
     * Найти позиции с включенным trailing stop
     */
    List<Position> findByTrailingStopEnabledTrueAndIsActiveTrueOrderByOpenedAtDesc();

    /**
     * Найти позиции без стоп-лосса (критическая ситуация)
     */
    @Query("SELECT p FROM Position p WHERE p.isActive = true AND p.stopLossPrice IS NULL ORDER BY p.openedAt ASC")
    List<Position> findPositionsWithoutStopLoss();

    // === Корреляционный анализ ===

    /**
     * Найти одновременные позиции (для анализа корреляции)
     */
    @Query("SELECT p FROM Position p WHERE p.isActive = true " +
            "AND p.tradingPair IN :pairs ORDER BY p.openedAt ASC")
    List<Position> findSimultaneousPositionsInPairs(@Param("pairs") List<String> pairs);

    /**
     * Найти позиции открытые в определенный период
     */
    @Query("SELECT p FROM Position p WHERE p.openedAt BETWEEN :start AND :end ORDER BY p.openedAt ASC")
    List<Position> findPositionsOpenedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Получить распределение позиций по типам пар
     */
    @Query("SELECT p.pairType, COUNT(p), SUM(p.entryValue) FROM Position p WHERE p.isActive = true GROUP BY p.pairType")
    List<Object[]> getActivePositionDistributionByPairType();

    // === Статистика по торговым парам ===

    /**
     * Получить статистику по торговым парам
     */
    @Query("SELECT p.tradingPair, " +
            "COUNT(p) as totalPositions, " +
            "SUM(CASE WHEN p.realizedPnl > 0 THEN 1 ELSE 0 END) as profitablePositions, " +
            "AVG(p.realizedPnl) as avgPnl, " +
            "SUM(p.realizedPnl) as totalPnl " +
            "FROM Position p WHERE p.isActive = false AND p.realizedPnl IS NOT NULL " +
            "GROUP BY p.tradingPair ORDER BY totalPnl DESC")
    List<Object[]> getPositionStatisticsByPair();

    /**
     * Найти наиболее активные торговые пары
     */
    @Query("SELECT p.tradingPair, COUNT(p) as positionCount " +
            "FROM Position p WHERE p.openedAt >= :since " +
            "GROUP BY p.tradingPair ORDER BY positionCount DESC")
    List<Object[]> getMostActiveTradin gPairsSince(@Param("since") LocalDateTime since);

    // === Управление позициями ===

    /**
     * Обновить текущую цену для всех активных позиций по торговой паре
     */
    @Modifying
    @Query("UPDATE Position p SET p.currentPrice = :price, p.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE p.tradingPair = :pair AND p.isActive = true")
    int updateCurrentPriceForPair(@Param("pair") String tradingPair, @Param("price") BigDecimal price);

    /**
     * Закрыть все позиции (аварийная остановка)
     */
    @Modifying
    @Query("UPDATE Position p SET p.isActive = false, p.status = 'CLOSED', " +
            "p.closedAt = CURRENT_TIMESTAMP, p.closeReason = :reason " +
            "WHERE p.isActive = true")
    int closeAllPositions(@Param("reason") String reason);

    /**
     * Закрыть позиции по торговой паре
     */
    @Modifying
    @Query("UPDATE Position p SET p.isActive = false, p.status = 'CLOSED', " +
            "p.closedAt = CURRENT_TIMESTAMP, p.closeReason = :reason " +
            "WHERE p.tradingPair = :pair AND p.isActive = true")
    int closePositionsForPair(@Param("pair") String tradingPair, @Param("reason") String reason);

    /**
     * Обновить уровень риска для позиции
     */
    @Modifying
    @Query("UPDATE Position p SET p.riskLevel = :riskLevel, p.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE p.id = :positionId")
    int updateRiskLevel(@Param("positionId") Long positionId, @Param("riskLevel") RiskLevel riskLevel);

    // === Быстрые проверки ===

    /**
     * Проверить наличие активных позиций
     */
    @Query("SELECT COUNT(p) > 0 FROM Position p WHERE p.isActive = true")
    boolean hasActivePositions();

    /**
     * Проверить наличие активной позиции по паре
     */
    @Query("SELECT COUNT(p) > 0 FROM Position p WHERE p.tradingPair = :pair AND p.isActive = true")
    boolean hasActivePositionForPair(@Param("pair") String tradingPair);

    /**
     * Проверить наличие просроченных позиций
     */
    @Query("SELECT COUNT(p) > 0 FROM Position p WHERE p.isActive = true AND p.forceCloseAt < CURRENT_TIMESTAMP")
    boolean hasExpiredPositions();

    /**
     * Получить количество позиций по направлению
     */
    @Query("SELECT p.side, COUNT(p) FROM Position p WHERE p.isActive = true GROUP BY p.side")
    List<Object[]> getActivePositionCountBySide();

    // === Расчет производительности ===

    /**
     * Рассчитать коэффициент выигрышных позиций
     */
    @Query("SELECT CAST(SUM(CASE WHEN p.realizedPnl > 0 THEN 1 ELSE 0 END) AS DOUBLE) / COUNT(p) * 100 " +
            "FROM Position p WHERE p.isActive = false AND p.realizedPnl IS NOT NULL")
    Double calculateWinRate();

    /**
     * Рассчитать средний profit factor
     */
    @Query("SELECT ABS(SUM(CASE WHEN p.realizedPnl > 0 THEN p.realizedPnl ELSE 0 END)) / " +
            "ABS(SUM(CASE WHEN p.realizedPnl < 0 THEN p.realizedPnl ELSE 0 END)) " +
            "FROM Position p WHERE p.isActive = false AND p.realizedPnl IS NOT NULL")
    Double calculateProfitFactor();

    /**
     * Рассчитать максимальную просадку
     */
    @Query("SELECT MIN(p.maxDrawdown) FROM Position p WHERE p.maxDrawdown IS NOT NULL")
    BigDecimal calculateMaxDrawdown();

    /**
     * Получить статистику времени удержания
     */
    @Query("SELECT AVG(FUNCTION('TIMESTAMPDIFF', MINUTE, p.openedAt, p.closedAt)), " +
            "MIN(FUNCTION('TIMESTAMPDIFF', MINUTE, p.openedAt, p.closedAt)), " +
            "MAX(FUNCTION('TIMESTAMPDIFF', MINUTE, p.openedAt, p.closedAt)) " +
            "FROM Position p WHERE p.isActive = false AND p.closedAt IS NOT NULL")
    Object[] getHoldingTimeStatistics();

    // === Очистка данных ===

    /**
     * Найти позиции для архивации
     */
    @Query("SELECT p FROM Position p WHERE p.isActive = false AND p.closedAt < :threshold ORDER BY p.closedAt ASC")
    List<Position> findPositionsForArchiving(@Param("threshold") LocalDateTime threshold);

    /**
     * Удалить старые закрытые позиции
     */
    @Modifying
    @Query("DELETE FROM Position p WHERE p.isActive = false AND p.closedAt < :threshold")
    int deleteOldClosedPositions(@Param("threshold") LocalDateTime threshold);

    // === Поиск для оптимизации ===

    /**
     * Найти позиции с наибольшим временем удержания
     */
    @Query("SELECT p FROM Position p WHERE p.isActive = false " +
            "ORDER BY FUNCTION('TIMESTAMPDIFF', MINUTE, p.openedAt, p.closedAt) DESC")
    List<Position> findLongestHeldPositions(Pageable pageable);

    /**
     * Найти наиболее волатильные позиции
     */
    @Query("SELECT p FROM Position p WHERE p.entryVolatility IS NOT NULL " +
            "ORDER BY p.entryVolatility DESC")
    List<Position> findMostVolatilePositions(Pageable pageable);

    /**
     * Найти позиции с лучшим соотношением риск/прибыль
     */
    @Query("SELECT p FROM Position p WHERE p.isActive = false AND p.realizedPnl > 0 " +
            "AND p.stopLossPrice IS NOT NULL AND p.entryPrice IS NOT NULL " +
            "ORDER BY (p.realizedPnl / ABS(p.entryPrice - p.stopLossPrice)) DESC")
    List<Position> findBestRiskRewardPositions(Pageable pageable);
}