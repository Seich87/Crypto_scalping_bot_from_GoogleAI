package com.example.scalpingBot.repository;

import com.example.scalpingBot.entity.MarketData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA репозиторий для сущности MarketData.
 * Предоставляет методы для сохранения и извлечения исторических рыночных данных.
 */
@Repository
public interface MarketDataRepository extends JpaRepository<MarketData, Long> {

    /**
     * Находит последние N записей рыночных данных для указанной торговой пары.
     * Используется для получения самой свежей информации для анализа.
     * Примечание: Spring Data JPA не поддерживает "LIMIT" напрямую в именах методов,
     * для этого используется Pageable. Для простоты здесь показан более общий метод.
     * Конкретное ограничение количества будет реализовано в сервисном слое с помощью Pageable.
     *
     * @param tradingPair Символ торговой пары.
     * @param start       Временная метка, после которой нужно искать данные.
     * @return Список рыночных данных, отсортированный по времени.
     */
    List<MarketData> findAllByTradingPairAndTimestampAfterOrderByTimestampAsc(String tradingPair, LocalDateTime start);

    /**
     * Находит все записи рыночных данных для торговой пары в указанном временном интервале.
     * Этот метод идеально подходит для анализа поведения рынка за конкретный период.
     *
     * @param tradingPair Символ торговой пары.
     * @param start       Начало временного интервала.
     * @param end         Конец временного интервала.
     * @return Список рыночных данных, отсортированный по времени.
     */
    List<MarketData> findAllByTradingPairAndTimestampBetweenOrderByTimestampAsc(String tradingPair, LocalDateTime start, LocalDateTime end);
}