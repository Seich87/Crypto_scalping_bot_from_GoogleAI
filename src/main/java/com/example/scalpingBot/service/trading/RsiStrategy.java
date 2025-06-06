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
public class RsiStrategy implements TradingStrategy {
    private static final Logger log = LoggerFactory.getLogger(RsiStrategy.class);
    public static final String NAME = "RSI_MEAN_REVERSION";

    private final MarketDataService marketDataService;
    private final PriceAnalyzer priceAnalyzer;

    public RsiStrategy(MarketDataService marketDataService, PriceAnalyzer priceAnalyzer) {
        this.marketDataService = marketDataService;
        this.priceAnalyzer = priceAnalyzer;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Optional<OrderSide> generateSignal(String symbol, Map<String, String> params) {
        int period = Integer.parseInt(params.getOrDefault("period", "14"));
        BigDecimal oversold = new BigDecimal(params.getOrDefault("oversoldLevel", "30"));
        BigDecimal overbought = new BigDecimal(params.getOrDefault("overboughtLevel", "70"));
        int dataFetchHours = Integer.parseInt(params.getOrDefault("dataFetchHours", "24"));

        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(dataFetchHours);
        List<MarketData> data = marketDataService.getHistoricalData(symbol, startTime, endTime);

        if (data.size() < period + 1) {
            return Optional.empty();
        }

        Optional<BigDecimal> rsiOpt = priceAnalyzer.calculateRSI(data, period);
        if (rsiOpt.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal rsi = rsiOpt.get();
        if (rsi.compareTo(oversold) < 0) {
            log.info("[{}] BUY signal for {}. RSI ({}) is below oversold level ({}).", getName(), symbol, rsi, oversold);
            return Optional.of(OrderSide.BUY);
        }
        if (rsi.compareTo(overbought) > 0) {
            log.info("[{}] SELL signal for {}. RSI ({}) is above overbought level ({}).", getName(), symbol, rsi, overbought);
            return Optional.of(OrderSide.SELL);
        }
        return Optional.empty();
    }
}