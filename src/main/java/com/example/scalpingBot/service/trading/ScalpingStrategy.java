package com.example.scalpingBot.service.trading;

import com.example.scalpingBot.entity.MarketData;
import com.example.scalpingBot.enums.OrderSide;
import com.example.scalpingBot.service.market.MarketDataService;
import com.example.scalpingBot.service.market.PriceAnalyzer;
import com.example.scalpingBot.service.market.VolumeAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Сервис, инкапсулирующий логику конкретной торговой стратегии.
 * Отвечает за генерацию торговых сигналов (BUY/SELL) на основе анализа рыночных данных.
 */
@Service
public class ScalpingStrategy {

    private static final Logger log = LoggerFactory.getLogger(ScalpingStrategy.class);

    private final MarketDataService marketDataService;
    private final PriceAnalyzer priceAnalyzer;
    private final VolumeAnalyzer volumeAnalyzer;

    // Параметры стратегии, загружаемые из application.properties
    private final int shortSmaPeriod;
    private final int longSmaPeriod;
    private final int dataFetchHours;

    public ScalpingStrategy(MarketDataService marketDataService,
                            PriceAnalyzer priceAnalyzer,
                            VolumeAnalyzer volumeAnalyzer,
                            @Value("${strategy.sma.short-period:10}") int shortSmaPeriod,
                            @Value("${strategy.sma.long-period:50}") int longSmaPeriod,
                            @Value("${strategy.data.fetch-hours:24}") int dataFetchHours) {
        this.marketDataService = marketDataService;
        this.priceAnalyzer = priceAnalyzer;
        this.volumeAnalyzer = volumeAnalyzer; // Пока не используется, но готово к расширению
        this.shortSmaPeriod = shortSmaPeriod;
        this.longSmaPeriod = longSmaPeriod;
        this.dataFetchHours = dataFetchHours;
        log.info("ScalpingStrategy initialized with shortSMA={}, longSMA={}", shortSmaPeriod, longSmaPeriod);
    }

    /**
     * Генерирует торговый сигнал для указанной торговой пары.
     *
     * @param symbol Торговая пара, например "BTCUSDT".
     * @return Optional, содержащий OrderSide (BUY или SELL) если сигнал есть, или пустой Optional.
     */
    public Optional<OrderSide> generateSignal(String symbol) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(dataFetchHours);
        List<MarketData> data = marketDataService.getHistoricalData(symbol, startTime, endTime);

        // Для расчета пересечения нам нужны данные как минимум за longSmaPeriod + 1
        if (data.size() < longSmaPeriod + 1) {
            log.warn("Not enough data for {}. Required at least {}, but got {}", symbol, longSmaPeriod + 1, data.size());
            return Optional.empty();
        }

        // Расчет SMA для текущего момента
        Optional<BigDecimal> currentShortSmaOpt = priceAnalyzer.calculateSMA(data, shortSmaPeriod);
        Optional<BigDecimal> currentLongSmaOpt = priceAnalyzer.calculateSMA(data, longSmaPeriod);

        // Расчет SMA для предыдущего момента (чтобы обнаружить сам факт пересечения)
        List<MarketData> previousData = data.subList(0, data.size() - 1);
        Optional<BigDecimal> prevShortSmaOpt = priceAnalyzer.calculateSMA(previousData, shortSmaPeriod);
        Optional<BigDecimal> prevLongSmaOpt = priceAnalyzer.calculateSMA(previousData, longSmaPeriod);

        if (currentShortSmaOpt.isEmpty() || currentLongSmaOpt.isEmpty() || prevShortSmaOpt.isEmpty() || prevLongSmaOpt.isEmpty()) {
            return Optional.empty(); // Не удалось рассчитать одну из SMA
        }

        BigDecimal currentShortSma = currentShortSmaOpt.get();
        BigDecimal currentLongSma = currentLongSmaOpt.get();
        BigDecimal prevShortSma = prevShortSmaOpt.get();
        BigDecimal prevLongSma = prevLongSmaOpt.get();

        // Логика стратегии: пересечение скользящих средних (SMA Crossover)
        // Сигнал на ПОКУПКУ ("Золотой крест"): короткая SMA пересекает длинную снизу вверх
        boolean isBuySignal = prevShortSma.compareTo(prevLongSma) <= 0 && currentShortSma.compareTo(currentLongSma) > 0;
        if (isBuySignal) {
            log.info("[{}] BUY signal generated. Short SMA ({}) crossed above Long SMA ({}).", symbol, currentShortSma, currentLongSma);
            return Optional.of(OrderSide.BUY);
        }

        // Сигнал на ПРОДАЖУ ("Крест смерти"): короткая SMA пересекает длинную сверху вниз
        boolean isSellSignal = prevShortSma.compareTo(prevLongSma) >= 0 && currentShortSma.compareTo(currentLongSma) < 0;
        if (isSellSignal) {
            log.info("[{}] SELL signal generated. Short SMA ({}) crossed below Long SMA ({}).", symbol, currentShortSma, currentLongSma);
            return Optional.of(OrderSide.SELL);
        }

        return Optional.empty(); // Нет сигнала
    }
}