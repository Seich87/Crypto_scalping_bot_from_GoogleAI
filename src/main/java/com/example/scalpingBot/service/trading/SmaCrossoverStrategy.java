package com.example.scalpingBot.service.trading;

import com.example.scalpingBot.entity.MarketData;
import com.example.scalpingBot.enums.OrderSide;
import com.example.scalpingBot.service.market.MarketDataService;
import com.example.scalpingBot.service.market.PriceAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SmaCrossoverStrategy implements TradingStrategy { // <-- Реализуем интерфейс

    private static final Logger log = LoggerFactory.getLogger(SmaCrossoverStrategy.class);
    public static final String NAME = "SMA_CROSSOVER";

    private final MarketDataService marketDataService;
    private final PriceAnalyzer priceAnalyzer;

    public SmaCrossoverStrategy(MarketDataService marketDataService, PriceAnalyzer priceAnalyzer) {
        this.marketDataService = marketDataService;
        this.priceAnalyzer = priceAnalyzer;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Optional<OrderSide> generateSignal(String symbol, Map<String, String> parameters) {
        // Извлекаем параметры из карты
        int shortSmaPeriod = Integer.parseInt(parameters.getOrDefault("shortSmaPeriod", "10"));
        int longSmaPeriod = Integer.parseInt(parameters.getOrDefault("longSmaPeriod", "50"));
        int dataFetchHours = Integer.parseInt(parameters.getOrDefault("dataFetchHours", "24"));

        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(dataFetchHours);
        List<MarketData> data = marketDataService.getHistoricalData(symbol, startTime, endTime);

        if (data.size() < longSmaPeriod + 1) {
            log.warn("[{}] Not enough data for {}. Required at least {}, but got {}", getName(), symbol, longSmaPeriod + 1, data.size());
            return Optional.empty();
        }

        Optional<BigDecimal> currentShortSmaOpt = priceAnalyzer.calculateSMA(data, shortSmaPeriod);
        Optional<BigDecimal> currentLongSmaOpt = priceAnalyzer.calculateSMA(data, longSmaPeriod);

        List<MarketData> previousData = data.subList(0, data.size() - 1);
        Optional<BigDecimal> prevShortSmaOpt = priceAnalyzer.calculateSMA(previousData, shortSmaPeriod);
        Optional<BigDecimal> prevLongSmaOpt = priceAnalyzer.calculateSMA(previousData, longSmaPeriod);

        if (currentShortSmaOpt.isEmpty() || currentLongSmaOpt.isEmpty() || prevShortSmaOpt.isEmpty() || prevLongSmaOpt.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal currentShortSma = currentShortSmaOpt.get();
        BigDecimal currentLongSma = currentLongSmaOpt.get();
        BigDecimal prevShortSma = prevShortSmaOpt.get();
        BigDecimal prevLongSma = prevLongSmaOpt.get();

        boolean isBuySignal = prevShortSma.compareTo(prevLongSma) <= 0 && currentShortSma.compareTo(currentLongSma) > 0;
        if (isBuySignal) {
            log.info("[{}] BUY signal generated for {}. Short SMA ({}) crossed above Long SMA ({}).", getName(), symbol, currentShortSma, currentLongSma);
            return Optional.of(OrderSide.BUY);
        }

        boolean isSellSignal = prevShortSma.compareTo(prevLongSma) >= 0 && currentShortSma.compareTo(currentLongSma) < 0;
        if (isSellSignal) {
            log.info("[{}] SELL signal generated for {}. Short SMA ({}) crossed below Long SMA ({}).", getName(), symbol, currentShortSma, currentLongSma);
            return Optional.of(OrderSide.SELL);
        }

        return Optional.empty();
    }
}