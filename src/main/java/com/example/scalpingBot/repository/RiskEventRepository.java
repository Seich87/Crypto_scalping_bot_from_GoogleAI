package com.example.scalpingBot.repository;

import com.example.scalpingBot.entity.RiskEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA репозиторий для сущности RiskEvent.
 * Предоставляет методы для сохранения и поиска событий, связанных с управлением рисками.
 */
@Repository
public interface RiskEventRepository extends JpaRepository<RiskEvent, Long> {

    /**
     * Находит все рисковые события, связанные с конкретной позицией,
     * отсортированные по времени (от новых к старым).
     *
     * @param positionId ID позиции.
     * @return Список рисковых событий.
     */
    List<RiskEvent> findAllByPositionIdOrderByEventTimestampDesc(Long positionId);

    /**
     * Находит все рисковые события определенного типа.
     * Например, можно найти все случаи срабатывания стоп-лосса.
     *
     * @param eventType Тип события.
     * @return Список рисковых событий.
     */
    List<RiskEvent> findAllByEventType(String eventType);

    /**
     * Находит все рисковые события, произошедшие в указанном временном интервале.
     *
     * @param start Начало временного интервала.
     * @param end   Конец временного интервала.
     * @return Список рисковых событий.
     */
    List<RiskEvent> findAllByEventTimestampBetween(LocalDateTime start, LocalDateTime end);
}