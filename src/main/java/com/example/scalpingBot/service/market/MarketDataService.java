package com.example.scalpingBot.service.market;

import com.example.scalpingBot.dto.exchange.TickerDto;
import com.example.scalpingBot.entity.MarketData;
import com.example.scalpingBot.exception.ExchangeApiException;
import com.example.scalpingBot.repository.MarketDataRepository;
import com.example.scalpingBot.service.exchange.ExchangeApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Сервис для управления рыночными данными.
 * Отвечает за получение свежих данных с биржи и их сохранение в базу данных,
 * а также за предоставление исторических данных другим сервисам.
 */
@Service
public class MarketDataService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);

    private final ExchangeApiService exchangeApiService;
    private final MarketDataRepository marketDataRepository;

    @Autowired
    public MarketDataService(@Qualifier("binance") ExchangeApiService exchangeApiService, // Внедряем конкретную реализацию
                             MarketDataRepository marketDataRepository) {
        this.exchangeApiService = exchangeApiService;
        this.marketDataRepository = marketDataRepository;
    }

    /**
     * Получает актуальный тикер (сводку рыночных данных) с биржи.
     *
     * @param symbol Торговая пара, например "BTCUSDT".
     * @return TickerDto с актуальными данными.
     * @throws ExchangeApiException если происходит ошибка при обращении к API биржи.
     */
    public TickerDto getCurrentTicker(String symbol) throws ExchangeApiException {
        return exchangeApiService.getTicker(symbol);
    }

    /**
     * Получает актуальный тикер с биржи и сохраняет его в базу данных.
     * Этот метод предназначен для вызова по расписанию (например, каждую минуту).
     *
     * @param symbol Торговая пара, для которой нужно сохранить данные.
     */
    @Transactional
    public void fetchAndSaveTicker(String symbol) {
        try {
            TickerDto tickerDto = getCurrentTicker(symbol);
            MarketData marketData = mapTickerToEntity(tickerDto);
            marketDataRepository.save(marketData);
            log.info("Successfully fetched and saved market data for {}", symbol);
        } catch (ExchangeApiException e) {
            log.error("Failed to fetch market data for {}: {}", symbol, e.getMessage());
            // Ошибка не пробрасывается дальше, чтобы не останавливать работу планировщика
        }
    }

    /**
     * Предоставляет исторические рыночные данные из базы данных за определенный период.
     *
     * @param symbol    Торговая пара.
     * @param startTime Начало временного интервала.
     * @param endTime   Конец временного интервала.
     * @return Список объектов MarketData в хронологическом порядке.
     */
    @Transactional(readOnly = true)
    public List<MarketData> getHistoricalData(String symbol, LocalDateTime startTime, LocalDateTime endTime) {
        return marketDataRepository.findAllByTradingPairAndTimestampBetweenOrderByTimestampAsc(symbol, startTime, endTime);
    }


    /**
     * Вспомогательный метод для преобразования DTO, полученного с биржи, в сущность для сохранения в БД.
     *
     * @param tickerDto DTO с данными от биржи.
     * @return Сущность MarketData, готовая к сохранению.
     */
    private MarketData mapTickerToEntity(TickerDto tickerDto) {
        return MarketData.builder()
                .tradingPair(tickerDto.getSymbol())
                .timestamp(tickerDto.getTimestamp())
                .lastPrice(tickerDto.getLastPrice())
                .bestBidPrice(tickerDto.getBestBidPrice())
                .bestAskPrice(tickerDto.getBestAskPrice())
                .volume24h(tickerDto.getVolume24h())
                .build();
    }
}