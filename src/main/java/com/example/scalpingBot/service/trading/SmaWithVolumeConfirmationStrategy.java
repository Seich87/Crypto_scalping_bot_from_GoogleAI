package com.example.scalpingBot.service.trading;

import com.example.scalpingBot.entity.MarketData;
import com.example.scalpingBot.enums.OrderSide;
import com.example.scalpingBot.service.market.MarketDataService;
import com.example.scalpingBot.service.market.PriceAnalyzer;
import com.example.scalpingBot.service.market.VolumeAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SmaWithVolumeConfirmationStrategy implements TradingStrategy {
    private static final Logger log = LoggerFactory.getLogger(SmaWithVolumeConfirmationStrategy.class);
    public static final String NAME = "SMA_VOLUME_CONFIRMED";

    private final MarketDataService marketDataService;
    private final PriceAnalyzer priceAnalyzer;
    private final VolumeAnalyzer volumeAnalyzer;

    public SmaWithVolumeConfirmationStrategy(MarketDataService marketDataService,
                                             PriceAnalyzer priceAnalyzer,
                                             VolumeAnalyzer volumeAnalyzer) {
        this.marketDataService = marketDataService;
        this.priceAnalyzer = priceAnalyzer;
        this.volumeAnalyzer = volumeAnalyzer;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Optional<OrderSide> generateSignal(String symbol, Map<String, String> params) {
        int shortSmaPeriod = Integer.parseInt(params.getOrDefault("shortSmaPeriod", "10"));
        int longSmaPeriod = Integer.parseInt(params.getOrDefault("longSmaPeriod", "50"));
        int volumePeriod = Integer.parseInt(params.getOrDefault("volumePeriod", "20"));
        double volumeFactor = Double.parseDouble(params.getOrDefault("volumeThresholdFactor", "2.0"));
        int dataFetchHours = Integer.parseInt(params.getOrDefault("dataFetchHours", "24"));

        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(dataFetchHours);
        List<MarketData> data = marketDataService.getHistoricalData(symbol, startTime, endTime);

        if (data.size() < longSmaPeriod + 1) {
            return Optional.empty();
        }

        // --- Логика SMA Crossover (как и раньше) ---
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
        boolean isSellSignal = prevShortSma.compareTo(prevLongSma) >= 0 && currentShortSma.compareTo(currentLongSma) < 0;

        // --- Подтверждение по объему ---
        if (isBuySignal || isSellSignal) {
            boolean isVolumeConfirmed = volumeAnalyzer.isVolumeSpike(data, volumePeriod, volumeFactor);
            if (isVolumeConfirmed) {
                if(isBuySignal) {
                    log.info("[{}] BUY signal for {} confirmed by volume spike.", getName(), symbol);
                    return Optional.of(OrderSide.BUY);
                } else {
                    log.info("[{}] SELL signal for {} confirmed by volume spike.", getName(), symbol);
                    return Optional.of(OrderSide.SELL);
                }
            } else {
                log.debug("[{}] Signal for {} ignored due to low volume.", getName(), symbol);
            }
        }

        return Optional.empty();
    }
}