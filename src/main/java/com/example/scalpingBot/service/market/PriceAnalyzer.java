package com.example.scalpingBot.service.market;

import com.example.scalpingBot.dto.BollingerBands;
import com.example.scalpingBot.dto.Macd;
import com.example.scalpingBot.entity.MarketData;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Сервис для анализа ценовых данных.
 * Предоставляет методы для расчета различных технических индикаторов.
 * Является чисто вычислительным и не имеет состояния (stateless).
 */
@Service
public class PriceAnalyzer {

    // Контекст для математических операций, чтобы избежать ошибок при делении
    private static final MathContext MC = new MathContext(32, RoundingMode.HALF_UP);

    /**
     * Рассчитывает простую скользящую среднюю (Simple Moving Average - SMA).
     */
    public Optional<BigDecimal> calculateSMA(List<MarketData> data, int period) {
        if (data == null || data.size() < period) {
            return Optional.empty();
        }
        List<MarketData> relevantData = data.subList(data.size() - period, data.size());
        BigDecimal sum = relevantData.stream()
                .map(MarketData::getLastPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Optional.of(sum.divide(new BigDecimal(period), MC));
    }

    /**
     * Рассчитывает индекс относительной силы (Relative Strength Index - RSI).
     */
    public Optional<BigDecimal> calculateRSI(List<MarketData> data, int period) {
        if (data == null || data.size() < period + 1) {
            return Optional.empty();
        }
        List<BigDecimal> gains = new ArrayList<>();
        List<BigDecimal> losses = new ArrayList<>();
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
            return Optional.of(new BigDecimal(100));
        }
        BigDecimal rs = averageGain.divide(averageLoss, MC);
        BigDecimal rsi = BigDecimal.valueOf(100).subtract(
                BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(rs), MC)
        );
        return Optional.of(rsi);
    }

    /**
     * Рассчитывает Полосы Боллинджера (Bollinger Bands).
     */
    public Optional<BollingerBands> calculateBollingerBands(List<MarketData> data, int period, BigDecimal stdDevMultiplier) {
        if (data == null || data.size() < period) {
            return Optional.empty();
        }
        Optional<BigDecimal> middleBandOpt = calculateSMA(data, period);
        if (middleBandOpt.isEmpty()) {
            return Optional.empty();
        }
        BigDecimal middleBand = middleBandOpt.get();
        List<MarketData> relevantData = data.subList(data.size() - period, data.size());
        BigDecimal sumOfSquares = relevantData.stream()
                .map(MarketData::getLastPrice)
                .map(price -> price.subtract(middleBand).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal variance = sumOfSquares.divide(new BigDecimal(period), MC);
        BigDecimal standardDeviation = new BigDecimal(Math.sqrt(variance.doubleValue()), MC);
        BigDecimal deviationAmount = standardDeviation.multiply(stdDevMultiplier);
        BigDecimal upperBand = middleBand.add(deviationAmount);
        BigDecimal lowerBand = middleBand.subtract(deviationAmount);
        return Optional.of(new BollingerBands(upperBand, middleBand, lowerBand));
    }

    /**
     * Рассчитывает индикатор MACD (Moving Average Convergence/Divergence).
     *
     * @param data         Список рыночных данных.
     * @param fastPeriod   Период для быстрой EMA (например, 12).
     * @param slowPeriod   Период для медленной EMA (например, 26).
     * @param signalPeriod Период для сигнальной линии (например, 9).
     * @return Optional, содержащий объект Macd, или пустой, если данных недостаточно.
     */
    public Optional<Macd> calculateMACD(List<MarketData> data, int fastPeriod, int slowPeriod, int signalPeriod) {
        if (data == null || data.size() < slowPeriod) {
            return Optional.empty();
        }
        List<BigDecimal> prices = data.stream().map(MarketData::getLastPrice).collect(Collectors.toList());

        List<BigDecimal> fastEmaSeries = calculateFullEmaSeries(prices, fastPeriod);
        List<BigDecimal> slowEmaSeries = calculateFullEmaSeries(prices, slowPeriod);

        // Линия MACD - это разница между быстрой и медленной EMA
        List<BigDecimal> macdLineSeries = IntStream.range(0, slowEmaSeries.size())
                .mapToObj(i -> fastEmaSeries.get(i + (slowPeriod - fastPeriod)).subtract(slowEmaSeries.get(i)))
                .collect(Collectors.toList());

        if (macdLineSeries.size() < signalPeriod) {
            return Optional.empty();
        }

        // Сигнальная линия - это EMA от линии MACD
        List<BigDecimal> signalLineSeries = calculateFullEmaSeries(macdLineSeries, signalPeriod);

        // Получаем последние значения
        BigDecimal lastMacdLine = macdLineSeries.get(macdLineSeries.size() - 1);
        BigDecimal lastSignalLine = signalLineSeries.get(signalLineSeries.size() - 1);
        BigDecimal histogram = lastMacdLine.subtract(lastSignalLine);

        return Optional.of(new Macd(lastMacdLine, lastSignalLine, histogram));
    }

    /**
     * Вспомогательный приватный метод для расчета всей серии EMA.
     * Это необходимо для построения истории индикаторов, на основе которой
     * рассчитываются другие, более сложные индикаторы (например, MACD).
     *
     * @param values Список значений (например, цен).
     * @param period Период EMA.
     * @return Полная история значений EMA.
     */
    private List<BigDecimal> calculateFullEmaSeries(List<BigDecimal> values, int period) {
        if (values.size() < period) {
            return new ArrayList<>();
        }
        List<BigDecimal> emaSeries = new ArrayList<>();
        BigDecimal multiplier = new BigDecimal(2).divide(new BigDecimal(period + 1), MC);

        // Рассчитываем SMA для первого значения
        BigDecimal initialSma = values.subList(0, period).stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(period), MC);
        emaSeries.add(initialSma);

        // Рассчитываем остальные значения EMA
        for (int i = period; i < values.size(); i++) {
            BigDecimal currentEma = values.get(i).subtract(emaSeries.get(emaSeries.size() - 1))
                    .multiply(multiplier)
                    .add(emaSeries.get(emaSeries.size() - 1));
            emaSeries.add(currentEma);
        }
        return emaSeries;
    }
}