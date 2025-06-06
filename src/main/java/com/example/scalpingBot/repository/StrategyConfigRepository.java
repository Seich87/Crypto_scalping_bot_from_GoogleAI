package com.example.scalpingBot.repository;

import com.example.scalpingBot.entity.StrategyConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StrategyConfigRepository extends JpaRepository<StrategyConfig, Long> {

    Optional<StrategyConfig> findByTradingPair(String tradingPair);
}