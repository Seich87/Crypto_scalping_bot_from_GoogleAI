package com.example.scalpingBot.service.market;

import com.example.scalpingBot.entity.MarketData;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * Сервис для анализа торговых объемов.
 * Предоставляет методы для выявления аномальных всплесков объема.
 * Является чисто вычислительным и не имеет состояния.
 */
@Service
public class VolumeAnalyzer {

    private static final MathContext MC = new MathContext(32, RoundingMode.HALF_UP);

    /**
     * Проверяет, был ли последний объем торгов аномально высоким по сравнению со средним за период.
     *
     * @param data           Список рыночных данных, отсортированный по возрастанию времени.
     * @param period         Период для расчета среднего объема (например, 20).
     * @param thresholdFactor Коэффициент для определения аномалии. Например, значение 2.0 будет
     *                       считать аномалией объем, который в 2 раза превышает средний.
     * @return true, если последний объем аномально высокий, иначе false.
     */
    public boolean isVolumeSpike(List<MarketData> data, int period, double thresholdFactor) {
        if (data == null || data.size() < period) {
            return false; // Недостаточно данных для анализа
        }

        // Вычисляем средний объем за период, исключая последнюю свечу
        List<MarketData> historicalData = data.subList(data.size() - period, data.size() - 1);
        Optional<BigDecimal> averageVolumeOpt = calculateAverageVolume(historicalData);

        if (averageVolumeOpt.isEmpty()) {
            return false;
        }

        BigDecimal averageVolume = averageVolumeOpt.get();
        // Объемы могут приходить как в базовой, так и в квотируемой валюте.
        // Здесь для простоты мы используем volume24h, но в реальной системе
        // нужно было бы работать с объемом за конкретный таймфрейм (например, 5-минутный).
        // Для демонстрации принципа этого достаточно.
        BigDecimal lastVolume = data.get(data.size() - 1).getVolume24h();

        // Порог для аномалии = средний объем * коэффициент
        BigDecimal volumeThreshold = averageVolume.multiply(BigDecimal.valueOf(thresholdFactor));

        return lastVolume.compareTo(volumeThreshold) > 0;
    }

    /**
     * Рассчитывает средний объем за указанный период.
     *
     * @param data Список рыночных данных.
     * @return Optional, содержащий средний объем, или пустой, если список пуст.
     */
    public Optional<BigDecimal> calculateAverageVolume(List<MarketData> data) {
        if (data == null || data.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal totalVolume = data.stream()
                .map(MarketData::getVolume24h)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Optional.of(totalVolume.divide(new BigDecimal(data.size()), MC));
    }
}