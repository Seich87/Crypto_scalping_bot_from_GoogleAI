package com.example.scalpingBot.utils;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

/**
 * Математические утилиты для скальпинг-бота
 *
 * Основные функции:
 * - Точные вычисления с BigDecimal
 * - Технические индикаторы (RSI, MACD, Bollinger Bands, EMA, ATR)
 * - Расчеты риска и позиционирования
 * - Статистические функции
 * - Процентные вычисления для торговли
 *
 * Все расчеты оптимизированы для скальпинг-стратегии
 * с учетом требований точности и производительности.
 *
 * @author ScalpingBot Team
 * @version 1.0
 */
@Slf4j
public class MathUtils {

    /**
     * Контекст вычислений с высокой точностью (16 знаков)
     */
    public static final MathContext HIGH_PRECISION = new MathContext(16, RoundingMode.HALF_UP);

    /**
     * Контекст вычислений для финансовых операций (8 знаков)
     */
    public static final MathContext FINANCIAL_PRECISION = new MathContext(8, RoundingMode.HALF_UP);

    /**
     * Контекст для процентных вычислений (4 знака)
     */
    public static final MathContext PERCENTAGE_PRECISION = new MathContext(4, RoundingMode.HALF_UP);

    /**
     * Константы
     */
    public static final BigDecimal ZERO = BigDecimal.ZERO;
    public static final BigDecimal ONE = BigDecimal.ONE;
    public static final BigDecimal TWO = new BigDecimal("2");
    public static final BigDecimal HUNDRED = new BigDecimal("100");

    /**
     * Минимальное значение для предотвращения деления на ноль
     */
    public static final BigDecimal EPSILON = new BigDecimal("0.0000001");

    // Приватный конструктор для утилитарного класса
    private MathUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Безопасное деление с проверкой на ноль
     *
     * @param dividend делимое
     * @param divisor делитель
     * @param defaultValue значение по умолчанию при делении на ноль
     * @return результат деления или значение по умолчанию
     */
    public static BigDecimal safeDivide(BigDecimal dividend, BigDecimal divisor, BigDecimal defaultValue) {
        if (divisor == null || divisor.compareTo(EPSILON) < 0) {
            return defaultValue;
        }
        return dividend.divide(divisor, HIGH_PRECISION);
    }

    /**
     * Безопасное деление с возвратом нуля при ошибке
     *
     * @param dividend делимое
     * @param divisor делитель
     * @return результат деления или ноль
     */
    public static BigDecimal safeDivide(BigDecimal dividend, BigDecimal divisor) {
        return safeDivide(dividend, divisor, ZERO);
    }

    /**
     * Вычислить процентное изменение
     *
     * @param oldValue старое значение
     * @param newValue новое значение
     * @return процентное изменение
     */
    public static BigDecimal percentageChange(BigDecimal oldValue, BigDecimal newValue) {
        if (oldValue == null || oldValue.compareTo(EPSILON) < 0) {
            return ZERO;
        }

        BigDecimal change = newValue.subtract(oldValue);
        return change.divide(oldValue, PERCENTAGE_PRECISION).multiply(HUNDRED);
    }

    /**
     * Рассчитать процент от суммы
     *
     * @param amount сумма
     * @param percentage процент
     * @return процент от суммы
     */
    public static BigDecimal percentageOf(BigDecimal amount, BigDecimal percentage) {
        return amount.multiply(percentage).divide(HUNDRED, FINANCIAL_PRECISION);
    }

    /**
     * Округлить до указанного количества знаков после запятой
     *
     * @param value значение для округления
     * @param scale количество знаков после запятой
     * @return округленное значение
     */
    public static BigDecimal round(BigDecimal value, int scale) {
        return value.setScale(scale, RoundingMode.HALF_UP);
    }

    /**
     * Округлить цену до точности торговой пары
     *
     * @param price цена
     * @param tickSize минимальный шаг цены
     * @return округленная цена
     */
    public static BigDecimal roundToTickSize(BigDecimal price, BigDecimal tickSize) {
        if (tickSize.compareTo(ZERO) <= 0) {
            return price;
        }

        BigDecimal divided = price.divide(tickSize, 0, RoundingMode.HALF_UP);
        return divided.multiply(tickSize);
    }

    /**
     * Округлить количество до размера лота
     *
     * @param quantity количество
     * @param lotSize размер лота
     * @return округленное количество
     */
    public static BigDecimal roundToLotSize(BigDecimal quantity, BigDecimal lotSize) {
        if (lotSize.compareTo(ZERO) <= 0) {
            return quantity;
        }

        BigDecimal divided = quantity.divide(lotSize, 0, RoundingMode.DOWN);
        return divided.multiply(lotSize);
    }

    /**
     * Проверить, находится ли значение в диапазоне
     *
     * @param value проверяемое значение
     * @param min минимальное значение
     * @param max максимальное значение
     * @return true если значение в диапазоне
     */
    public static boolean isInRange(BigDecimal value, BigDecimal min, BigDecimal max) {
        return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }

    /**
     * Ограничить значение диапазоном
     *
     * @param value значение
     * @param min минимум
     * @param max максимум
     * @return ограниченное значение
     */
    public static BigDecimal clamp(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value.compareTo(min) < 0) return min;
        if (value.compareTo(max) > 0) return max;
        return value;
    }

    /**
     * Найти минимальное значение в списке
     *
     * @param values список значений
     * @return минимальное значение или null если список пуст
     */
    public static BigDecimal min(List<BigDecimal> values) {
        return values.stream()
                .filter(v -> v != null)
                .min(BigDecimal::compareTo)
                .orElse(null);
    }

    /**
     * Найти максимальное значение в списке
     *
     * @param values список значений
     * @return максимальное значение или null если список пуст
     */
    public static BigDecimal max(List<BigDecimal> values) {
        return values.stream()
                .filter(v -> v != null)
                .max(BigDecimal::compareTo)
                .orElse(null);
    }

    /**
     * Рассчитать среднее арифметическое
     *
     * @param values список значений
     * @return среднее значение
     */
    public static BigDecimal average(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return ZERO;
        }

        BigDecimal sum = values.stream()
                .filter(v -> v != null)
                .reduce(ZERO, BigDecimal::add);

        return safeDivide(sum, new BigDecimal(values.size()));
    }

    /**
     * Рассчитать стандартное отклонение
     *
     * @param values список значений
     * @return стандартное отклонение
     */
    public static BigDecimal standardDeviation(List<BigDecimal> values) {
        if (values == null || values.size() < 2) {
            return ZERO;
        }

        BigDecimal mean = average(values);
        BigDecimal sumSquaredDiffs = ZERO;

        for (BigDecimal value : values) {
            if (value != null) {
                BigDecimal diff = value.subtract(mean);
                sumSquaredDiffs = sumSquaredDiffs.add(diff.multiply(diff));
            }
        }

        BigDecimal variance = safeDivide(sumSquaredDiffs, new BigDecimal(values.size() - 1));
        return sqrt(variance);
    }

    /**
     * Приближенное вычисление квадратного корня методом Ньютона
     *
     * @param value значение
     * @return квадратный корень
     */
    public static BigDecimal sqrt(BigDecimal value) {
        if (value.compareTo(ZERO) <= 0) {
            return ZERO;
        }

        if (value.compareTo(ONE) == 0) {
            return ONE;
        }

        BigDecimal x = value;
        BigDecimal result = value.divide(TWO, HIGH_PRECISION);

        // Метод Ньютона: x_{n+1} = (x_n + value/x_n) / 2
        for (int i = 0; i < 50; i++) {
            x = result;
            result = x.add(value.divide(x, HIGH_PRECISION)).divide(TWO, HIGH_PRECISION);

            // Проверка сходимости
            if (x.subtract(result).abs().compareTo(EPSILON) < 0) {
                break;
            }
        }

        return result;
    }

    /**
     * Рассчитать RSI (Relative Strength Index)
     *
     * @param closes список цен закрытия
     * @param period период для расчета (обычно 14)
     * @return значение RSI (0-100)
     */
    public static BigDecimal calculateRSI(List<BigDecimal> closes, int period) {
        if (closes == null || closes.size() < period + 1) {
            return new BigDecimal("50"); // Нейтральное значение
        }

        BigDecimal sumGains = ZERO;
        BigDecimal sumLosses = ZERO;

        // Рассчитываем изменения цен
        for (int i = 1; i <= period; i++) {
            BigDecimal change = closes.get(i).subtract(closes.get(i - 1));
            if (change.compareTo(ZERO) > 0) {
                sumGains = sumGains.add(change);
            } else {
                sumLosses = sumLosses.add(change.abs());
            }
        }

        BigDecimal avgGain = safeDivide(sumGains, new BigDecimal(period));
        BigDecimal avgLoss = safeDivide(sumLosses, new BigDecimal(period));

        if (avgLoss.compareTo(EPSILON) < 0) {
            return HUNDRED; // Все движения положительные
        }

        BigDecimal rs = avgGain.divide(avgLoss, HIGH_PRECISION);
        BigDecimal rsi = HUNDRED.subtract(HUNDRED.divide(ONE.add(rs), HIGH_PRECISION));

        return round(rsi, 2);
    }

    /**
     * Рассчитать EMA (Exponential Moving Average)
     *
     * @param prices список цен
     * @param period период для расчета
     * @return значение EMA
     */
    public static BigDecimal calculateEMA(List<BigDecimal> prices, int period) {
        if (prices == null || prices.isEmpty()) {
            return ZERO;
        }

        if (prices.size() == 1) {
            return prices.get(0);
        }

        BigDecimal multiplier = TWO.divide(new BigDecimal(period + 1), HIGH_PRECISION);
        BigDecimal ema = prices.get(0); // Начинаем с первой цены

        for (int i = 1; i < prices.size(); i++) {
            ema = prices.get(i).multiply(multiplier).add(ema.multiply(ONE.subtract(multiplier)));
        }

        return round(ema, 8);
    }

    /**
     * Рассчитать Bollinger Bands
     *
     * @param prices список цен
     * @param period период для расчета (обычно 20)
     * @param stdDevMultiplier множитель стандартного отклонения (обычно 2)
     * @return массив [нижняя полоса, средняя линия, верхняя полоса]
     */
    public static BigDecimal[] calculateBollingerBands(List<BigDecimal> prices, int period, BigDecimal stdDevMultiplier) {
        if (prices == null || prices.size() < period) {
            BigDecimal middle = prices != null && !prices.isEmpty() ? prices.get(prices.size() - 1) : ZERO;
            return new BigDecimal[]{middle, middle, middle};
        }

        List<BigDecimal> recentPrices = prices.subList(prices.size() - period, prices.size());
        BigDecimal middle = average(recentPrices); // SMA
        BigDecimal stdDev = standardDeviation(recentPrices);

        BigDecimal deviation = stdDev.multiply(stdDevMultiplier);
        BigDecimal upper = middle.add(deviation);
        BigDecimal lower = middle.subtract(deviation);

        return new BigDecimal[]{
                round(lower, 8),    // Нижняя полоса
                round(middle, 8),   // Средняя линия (SMA)
                round(upper, 8)     // Верхняя полоса
        };
    }

    /**
     * Рассчитать MACD (Moving Average Convergence Divergence)
     *
     * @param prices список цен
     * @param fastPeriod быстрый период (обычно 12)
     * @param slowPeriod медленный период (обычно 26)
     * @param signalPeriod период сигнальной линии (обычно 9)
     * @return массив [MACD линия, сигнальная линия, гистограмма]
     */
    public static BigDecimal[] calculateMACD(List<BigDecimal> prices, int fastPeriod, int slowPeriod, int signalPeriod) {
        if (prices == null || prices.size() < slowPeriod) {
            return new BigDecimal[]{ZERO, ZERO, ZERO};
        }

        BigDecimal fastEMA = calculateEMA(prices, fastPeriod);
        BigDecimal slowEMA = calculateEMA(prices, slowPeriod);
        BigDecimal macdLine = fastEMA.subtract(slowEMA);

        // Для сигнальной линии нужна история MACD значений
        // Упрощенная версия - используем текущее значение
        BigDecimal signalLine = macdLine; // В реальной реализации нужна история
        BigDecimal histogram = macdLine.subtract(signalLine);

        return new BigDecimal[]{
                round(macdLine, 8),
                round(signalLine, 8),
                round(histogram, 8)
        };
    }

    /**
     * Рассчитать ATR (Average True Range)
     *
     * @param highs список максимальных цен
     * @param lows список минимальных цен
     * @param closes список цен закрытия
     * @param period период для расчета (обычно 14)
     * @return значение ATR
     */
    public static BigDecimal calculateATR(List<BigDecimal> highs, List<BigDecimal> lows,
                                          List<BigDecimal> closes, int period) {
        if (highs == null || lows == null || closes == null ||
                highs.size() < period + 1 || lows.size() < period + 1 || closes.size() < period + 1) {
            return ZERO;
        }

        BigDecimal sumTR = ZERO;

        for (int i = 1; i <= period; i++) {
            BigDecimal high = highs.get(i);
            BigDecimal low = lows.get(i);
            BigDecimal prevClose = closes.get(i - 1);

            // True Range = max(high-low, abs(high-prevClose), abs(low-prevClose))
            BigDecimal tr1 = high.subtract(low);
            BigDecimal tr2 = high.subtract(prevClose).abs();
            BigDecimal tr3 = low.subtract(prevClose).abs();

            BigDecimal trueRange = max(Arrays.asList(tr1, tr2, tr3));
            sumTR = sumTR.add(trueRange);
        }

        return round(safeDivide(sumTR, new BigDecimal(period)), 8);
    }

    /**
     * Рассчитать размер позиции на основе риска
     *
     * @param accountBalance баланс счета
     * @param riskPercent процент риска на сделку
     * @param entryPrice цена входа
     * @param stopLossPrice цена стоп-лосса
     * @return размер позиции
     */
    public static BigDecimal calculatePositionSize(BigDecimal accountBalance, BigDecimal riskPercent,
                                                   BigDecimal entryPrice, BigDecimal stopLossPrice) {
        if (accountBalance == null || entryPrice == null || stopLossPrice == null) {
            return ZERO;
        }

        BigDecimal riskAmount = percentageOf(accountBalance, riskPercent);
        BigDecimal priceDifference = entryPrice.subtract(stopLossPrice).abs();

        if (priceDifference.compareTo(EPSILON) < 0) {
            return ZERO;
        }

        return safeDivide(riskAmount, priceDifference);
    }

    /**
     * Рассчитать P&L позиции
     *
     * @param quantity количество
     * @param entryPrice цена входа
     * @param currentPrice текущая цена
     * @param side сторона позиции (1 для long, -1 для short)
     * @return прибыль/убыток
     */
    public static BigDecimal calculatePnL(BigDecimal quantity, BigDecimal entryPrice,
                                          BigDecimal currentPrice, int side) {
        if (quantity == null || entryPrice == null || currentPrice == null) {
            return ZERO;
        }

        BigDecimal priceDiff = currentPrice.subtract(entryPrice);
        return quantity.multiply(priceDiff).multiply(new BigDecimal(side));
    }

    /**
     * Рассчитать P&L в процентах
     *
     * @param entryPrice цена входа
     * @param currentPrice текущая цена
     * @param side сторона позиции (1 для long, -1 для short)
     * @return P&L в процентах
     */
    public static BigDecimal calculatePnLPercent(BigDecimal entryPrice, BigDecimal currentPrice, int side) {
        if (entryPrice == null || currentPrice == null || entryPrice.compareTo(EPSILON) < 0) {
            return ZERO;
        }

        BigDecimal priceDiff = currentPrice.subtract(entryPrice);
        BigDecimal percentChange = safeDivide(priceDiff, entryPrice).multiply(HUNDRED);

        return percentChange.multiply(new BigDecimal(side));
    }

    /**
     * Рассчитать корреляцию между двумя рядами данных
     *
     * @param series1 первый ряд данных
     * @param series2 второй ряд данных
     * @return коэффициент корреляции (-1 до 1)
     */
    public static BigDecimal calculateCorrelation(List<BigDecimal> series1, List<BigDecimal> series2) {
        if (series1 == null || series2 == null || series1.size() != series2.size() || series1.size() < 2) {
            return ZERO;
        }

        BigDecimal mean1 = average(series1);
        BigDecimal mean2 = average(series2);

        BigDecimal numerator = ZERO;
        BigDecimal sumSq1 = ZERO;
        BigDecimal sumSq2 = ZERO;

        for (int i = 0; i < series1.size(); i++) {
            BigDecimal diff1 = series1.get(i).subtract(mean1);
            BigDecimal diff2 = series2.get(i).subtract(mean2);

            numerator = numerator.add(diff1.multiply(diff2));
            sumSq1 = sumSq1.add(diff1.multiply(diff1));
            sumSq2 = sumSq2.add(diff2.multiply(diff2));
        }

        BigDecimal denominator = sqrt(sumSq1.multiply(sumSq2));

        return round(safeDivide(numerator, denominator), 4);
    }

    /**
     * Рассчитать максимальную просадку
     *
     * @param equityCurve кривая капитала
     * @return максимальная просадка в процентах
     */
    public static BigDecimal calculateMaxDrawdown(List<BigDecimal> equityCurve) {
        if (equityCurve == null || equityCurve.size() < 2) {
            return ZERO;
        }

        BigDecimal maxDrawdown = ZERO;
        BigDecimal peak = equityCurve.get(0);

        for (BigDecimal value : equityCurve) {
            if (value.compareTo(peak) > 0) {
                peak = value;
            }

            BigDecimal drawdown = safeDivide(peak.subtract(value), peak).multiply(HUNDRED);
            if (drawdown.compareTo(maxDrawdown) > 0) {
                maxDrawdown = drawdown;
            }
        }

        return round(maxDrawdown, 2);
    }
}