package com.example.scalpingBot.service.trading;

import com.example.scalpingBot.dto.BollingerBands;
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
public class BollingerBandsStrategy implements TradingStrategy {

    private static final Logger log = LoggerFactory.getLogger(BollingerBandsStrategy.class);
    public static final String NAME = "BOLLINGER_BANDS";

    private final MarketDataService marketDataService;
    private final PriceAnalyzer priceAnalyzer;

    public BollingerBandsStrategy(MarketDataService marketDataService, PriceAnalyzer priceAnalyzer) {
        this.marketDataService = marketDataService;
        this.priceAnalyzer = priceAnalyzer;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Optional<OrderSide> generateSignal(String symbol, Map<String, String> parameters) {
        // Извлекаем параметры
        int period = Integer.parseInt(parameters.getOrDefault("period", "20"));
        BigDecimal stdDevMultiplier = new BigDecimal(parameters.getOrDefault("stdDevMultiplier", "2.0"));
        int dataFetchHours = Integer.parseInt(parameters.getOrDefault("dataFetchHours", "24"));

        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(dataFetchHours);
        List<MarketData> data = marketDataService.getHistoricalData(symbol, startTime, endTime);

        if (data.isEmpty()) {
            return Optional.empty();
        }

        Optional<BollingerBands> bandsOpt = priceAnalyzer.calculateBollingerBands(data, period, stdDevMultiplier);
        if (bandsOpt.isEmpty()) {
            return Optional.empty();
        }

        BollingerBands bands = bandsOpt.get();
        BigDecimal currentPrice = data.get(data.size() - 1).getLastPrice();

        // Стратегия: Покупка при касании нижней полосы, продажа при касании верхней
        // Это контртрендовая стратегия
        if (currentPrice.compareTo(bands.lowerBand()) <= 0) {
            log.info("[{}] BUY signal generated for {}. Price {} touched lower Bollinger Band {}.", getName(), symbol, currentPrice, bands.lowerBand());
            return Optional.of(OrderSide.BUY);
        }

        if (currentPrice.compareTo(bands.upperBand()) >= 0) {
            log.info("[{}] SELL signal generated for {}. Price {} touched upper Bollinger Band {}.", getName(), symbol, currentPrice, bands.upperBand());
            return Optional.of(OrderSide.SELL);
        }

        return Optional.empty();
    }
}