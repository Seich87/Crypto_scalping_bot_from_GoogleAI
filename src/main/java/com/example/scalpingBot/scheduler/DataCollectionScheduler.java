package com.example.scalpingBot.scheduler;

import com.example.scalpingBot.service.market.MarketDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Планировщик, отвечающий за периодический сбор рыночных данных.
 */
@Component
public class DataCollectionScheduler {

    private static final Logger log = LoggerFactory.getLogger(DataCollectionScheduler.class);

    private final MarketDataService marketDataService;
    private final List<String> tradingPairs;

    @Autowired
    public DataCollectionScheduler(MarketDataService marketDataService,
                                   @Value("${bot.trading-pairs}") List<String> tradingPairs) {
        this.marketDataService = marketDataService;
        this.tradingPairs = tradingPairs;
        log.info("DataCollectionScheduler initialized for pairs: {}", tradingPairs);
    }

    /**
     * Этот метод будет выполняться каждые 60 секунд.
     * fixedRate = 60000 миллисекунд.
     * Он собирает и сохраняет данные тикера для всех торговых пар, указанных в конфигурации.
     */
    @Scheduled(fixedRate = 60000, initialDelay = 10000)
    public void collectMarketData() {
        log.info("--- Starting market data collection task ---");
        if (tradingPairs.isEmpty()) {
            log.warn("No trading pairs configured. Skipping data collection.");
            return;
        }

        for (String pair : tradingPairs) {
            try {
                marketDataService.fetchAndSaveTicker(pair);
            } catch (Exception e) {
                // Логика обработки ошибок уже внутри marketDataService,
                // поэтому здесь можно просто продолжать работу.
                log.error("An unexpected error occurred while collecting data for {}", pair, e);
            }
        }
        log.info("--- Finished market data collection task ---");
    }
}