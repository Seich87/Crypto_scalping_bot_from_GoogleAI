package com.example.scalpingBot.service.market;

import com.example.scalpingBot.entity.MarketData;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Сервис для анализа ценовых данных.
 * Предоставляет методы для расчета различных технических индикаторов.
 * Является чисто вычислительным и не имеет состояния.
 */
@Service
public class PriceAnalyzer {

    // Контекст для математических операций, чтобы избежать ошибок при делении
    private static final MathContext MC = new MathContext(32, RoundingMode.HALF_UP);

    /**
     * Рассчитывает простую скользящую среднюю (Simple Moving Average - SMA).
     *
     * @param data   Список рыночных данных, отсортированный по возрастанию времени.
     * @param period Период для расчета SMA (например, 10, 50, 200).
     * @return Optional, содержащий значение SMA, или пустой Optional, если данных недостаточно.
     */
    public Optional<BigDecimal> calculateSMA(List<MarketData> data, int period) {
        if (data == null || data.size() < period) {
            return Optional.empty(); // Недостаточно данных для расчета
        }

        // Берем последние `period` элементов для расчета
        List<MarketData> relevantData = data.subList(data.size() - period, data.size());

        BigDecimal sum = relevantData.stream()
                .map(MarketData::getLastPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Optional.of(sum.divide(new BigDecimal(period), MC));
    }

    /**
     * Рассчитывает индекс относительной силы (Relative Strength Index - RSI).
     *
     * @param data   Список рыночных данных, отсортированный по возрастанию времени.
     * @param period Период для расчета RSI (классическое значение - 14).
     * @return Optional, содержащий значение RSI, или пустой Optional, если данных недостаточно.
     */
    public Optional<BigDecimal> calculateRSI(List<MarketData> data, int period) {
        // Для расчета RSI нам нужно как минимум 'period' + 1 точек данных,
        // чтобы иметь 'period' изменений цены.
        if (data == null || data.size() < period + 1) {
            return Optional.empty();
        }

        List<BigDecimal> gains = new ArrayList<>();
        List<BigDecimal> losses = new ArrayList<>();

        // Рассчитываем приросты и потери за 'period' последних периодов
        for (int i = data.size() - period; i < data.size(); i++) {
            BigDecimal difference = data.get(i).getLastPrice().subtract(data.get(i - 1).getLastPrice());
            if (difference.compareTo(BigDecimal.ZERO) > 0) {
                gains.add(difference);
                losses.add(BigDecimal.ZERO);
            } else {
                losses.add(difference.abs());
                gains.add(BigDecimal.ZERO);
            }
        }

        BigDecimal averageGain = gains.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(new BigDecimal(period), MC);
        BigDecimal averageLoss = losses.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(new BigDecimal(period), MC);

        if (averageLoss.compareTo(BigDecimal.ZERO) == 0) {
            return Optional.of(new BigDecimal(100)); // Если нет потерь, RSI равен 100
        }

        BigDecimal rs = averageGain.divide(averageLoss, MC); // Relative Strength
        BigDecimal rsi = BigDecimal.valueOf(100).subtract(
                BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(rs), MC)
        );

        return Optional.of(rsi);
    }
}