package com.example.scalpingBot.repository;

import com.example.scalpingBot.entity.TradingPair;
import com.example.scalpingBot.enums.TradingPairType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface TradingPairRepository extends JpaRepository<TradingPair, Long> {

    /**
     * Находит торговую пару по символу.
     */
    Optional<TradingPair> findBySymbol(String symbol);

    /**
     * Находит все активные торговые пары.
     */
    List<TradingPair> findAllByIsActive(boolean isActive);

    /**
     * Находит все торговые пары определенного типа.
     */
    List<TradingPair> findAllByType(TradingPairType type);

    /**
     * Находит активные торговые пары определенного типа.
     */
    List<TradingPair> findAllByIsActiveAndType(boolean isActive, TradingPairType type);

    /**
     * Находит торговые пары по базовому активу.
     */
    List<TradingPair> findAllByBaseAsset(String baseAsset);

    /**
     * Находит торговые пары по квотируемому активу.
     */
    List<TradingPair> findAllByQuoteAsset(String quoteAsset);

    /**
     * Находит активные пары с минимальным размером ордера меньше указанного.
     */
    @Query("SELECT tp FROM TradingPair tp WHERE tp.isActive = true AND tp.minOrderSize <= :maxSize")
    List<TradingPair> findActiveWithMinOrderSizeLessThan(@Param("maxSize") BigDecimal maxSize);

    /**
     * Проверяет существование активной торговой пары.
     */
    boolean existsBySymbolAndIsActive(String symbol, boolean isActive);
}