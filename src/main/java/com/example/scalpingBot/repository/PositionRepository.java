package com.example.scalpingBot.repository;

import com.example.scalpingBot.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA репозиторий для сущности Position.
 * Предоставляет методы для выполнения CRUD-операций и поиска позиций по различным критериям.
 */
@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {

    /**
     * Находит позицию по торговой паре и её статусу активности.
     * Основное применение - поиск текущей ОТКРЫТОЙ позиции для конкретной пары.
     *
     * @param tradingPair Символ торговой пары (например, "BTCUSDT").
     * @param isActive    Статус активности (true для открытых, false для закрытых).
     * @return Optional, содержащий позицию, если она найдена.
     */
    Optional<Position> findByTradingPairAndIsActive(String tradingPair, boolean isActive);

    /**
     * Находит все позиции с указанным статусом активности.
     * Например, можно получить список всех открытых позиций бота.
     *
     * @param isActive Статус активности.
     * @return Список позиций.
     */
    List<Position> findAllByIsActive(boolean isActive);

    /**
     * Находит все позиции для указанной торговой пары, отсортированные по времени закрытия.
     * Полезно для анализа истории торговли по конкретному активу.
     *
     * @param tradingPair Символ торговой пары.
     * @return Список всех позиций для данной пары.
     */
    List<Position> findAllByTradingPairOrderByCloseTimestampDesc(String tradingPair);

    // Внутри интерфейса PositionRepository

    /**
     * Находит все закрытые позиции, отсортированные по времени закрытия в хронологическом порядке.
     * Это необходимо для корректного построения кривой доходности и расчета просадки.
     *
     * @param isActive статус активности (должен быть false).
     * @return Список отсортированных закрытых позиций.
     */
    List<Position> findAllByIsActiveOrderByCloseTimestampAsc(boolean isActive);
}