package com.example.scalpingBot.service.event;

import com.example.scalpingBot.dto.exchange.TickerDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class MarketDataEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(MarketDataEventPublisher.class);

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MarketDataEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Публикует событие тикера на основе сообщения WebSocket от Binance.
     * Обрабатывает различные типы потоков: trade, ticker, miniTicker.
     */
    public void publishTickerEvent(String jsonMessage) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonMessage);

            // Проверяем тип сообщения
            if (rootNode.has("stream")) {
                // Это сообщение из combined stream
                handleCombinedStreamMessage(rootNode);
            } else if (rootNode.has("e")) {
                // Это прямое сообщение события
                handleDirectEventMessage(rootNode);
            } else {
                log.trace("Ignoring unknown message format: {}", jsonMessage);
            }

        } catch (JsonProcessingException e) {
            log.warn("Failed to parse WebSocket message: {}. Error: {}", jsonMessage, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error processing WebSocket message: {}", e.getMessage(), e);
        }
    }

    /**
     * Обрабатывает сообщения из combined stream (формат: {stream: "...", data: {...}})
     */
    private void handleCombinedStreamMessage(JsonNode rootNode) {
        String streamName = rootNode.path("stream").asText();
        JsonNode dataNode = rootNode.path("data");

        if (streamName.endsWith("@trade")) {
            handleTradeEvent(dataNode);
        } else if (streamName.endsWith("@ticker")) {
            handleTickerEvent(dataNode);
        } else if (streamName.endsWith("@miniTicker")) {
            handleMiniTickerEvent(dataNode);
        } else {
            log.trace("Ignoring stream: {}", streamName);
        }
    }

    /**
     * Обрабатывает прямые сообщения событий
     */
    private void handleDirectEventMessage(JsonNode eventNode) {
        String eventType = eventNode.path("e").asText();

        switch (eventType) {
            case "trade" -> handleTradeEvent(eventNode);
            case "24hrTicker" -> handleTickerEvent(eventNode);
            case "24hrMiniTicker" -> handleMiniTickerEvent(eventNode);
            default -> log.trace("Ignoring event type: {}", eventType);
        }
    }

    /**
     * Обрабатывает событие индивидуальной сделки
     */
    private void handleTradeEvent(JsonNode tradeNode) {
        try {
            String symbol = tradeNode.path("s").asText();
            BigDecimal price = new BigDecimal(tradeNode.path("p").asText());
            BigDecimal quantity = new BigDecimal(tradeNode.path("q").asText());
            long eventTime = tradeNode.path("E").asLong();

            TickerDto ticker = TickerDto.builder()
                    .symbol(symbol)
                    .lastPrice(price)
                    .volume24h(quantity) // Для trade события используем количество текущей сделки
                    .timestamp(convertTimestamp(eventTime))
                    .build();

            publishEvent(ticker);
            log.trace("Published trade event for {}: price={}, qty={}", symbol, price, quantity);

        } catch (Exception e) {
            log.warn("Failed to process trade event: {}", e.getMessage());
        }
    }

    /**
     * Обрабатывает полное событие 24hr ticker
     */
    private void handleTickerEvent(JsonNode tickerNode) {
        try {
            String symbol = tickerNode.path("s").asText();
            BigDecimal lastPrice = new BigDecimal(tickerNode.path("c").asText());
            BigDecimal openPrice = new BigDecimal(tickerNode.path("o").asText());
            BigDecimal highPrice = new BigDecimal(tickerNode.path("h").asText());
            BigDecimal lowPrice = new BigDecimal(tickerNode.path("l").asText());
            BigDecimal volume = new BigDecimal(tickerNode.path("v").asText());
            BigDecimal quoteVolume = new BigDecimal(tickerNode.path("q").asText());
            BigDecimal priceChange = new BigDecimal(tickerNode.path("P").asText());
            BigDecimal bidPrice = new BigDecimal(tickerNode.path("b").asText());
            BigDecimal askPrice = new BigDecimal(tickerNode.path("a").asText());
            long eventTime = tickerNode.path("E").asLong();

            TickerDto ticker = TickerDto.builder()
                    .symbol(symbol)
                    .lastPrice(lastPrice)
                    .bestBidPrice(bidPrice)
                    .bestAskPrice(askPrice)
                    .volume24h(volume)
                    .quoteVolume24h(quoteVolume)
                    .priceChangePercent24h(priceChange)
                    .timestamp(convertTimestamp(eventTime))
                    .build();

            publishEvent(ticker);
            log.trace("Published 24hr ticker event for {}: price={}, change={}%",
                    symbol, lastPrice, priceChange);

        } catch (Exception e) {
            log.warn("Failed to process 24hr ticker event: {}", e.getMessage());
        }
    }

    /**
     * Обрабатывает событие mini ticker (только основные данные)
     */
    private void handleMiniTickerEvent(JsonNode miniTickerNode) {
        try {
            String symbol = miniTickerNode.path("s").asText();
            BigDecimal lastPrice = new BigDecimal(miniTickerNode.path("c").asText());
            BigDecimal openPrice = new BigDecimal(miniTickerNode.path("o").asText());
            BigDecimal highPrice = new BigDecimal(miniTickerNode.path("h").asText());
            BigDecimal lowPrice = new BigDecimal(miniTickerNode.path("l").asText());
            BigDecimal volume = new BigDecimal(miniTickerNode.path("v").asText());
            BigDecimal quoteVolume = new BigDecimal(miniTickerNode.path("q").asText());
            long eventTime = miniTickerNode.path("E").asLong();

            // Рассчитываем процентное изменение
            BigDecimal priceChangePercent = BigDecimal.ZERO;
            if (openPrice.compareTo(BigDecimal.ZERO) > 0) {
                priceChangePercent = lastPrice.subtract(openPrice)
                        .divide(openPrice, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(new BigDecimal(100));
            }

            TickerDto ticker = TickerDto.builder()
                    .symbol(symbol)
                    .lastPrice(lastPrice)
                    .volume24h(volume)
                    .quoteVolume24h(quoteVolume)
                    .priceChangePercent24h(priceChangePercent)
                    .timestamp(convertTimestamp(eventTime))
                    .build();

            publishEvent(ticker);
            log.trace("Published mini ticker event for {}: price={}", symbol, lastPrice);

        } catch (Exception e) {
            log.warn("Failed to process mini ticker event: {}", e.getMessage());
        }
    }

    /**
     * Публикует событие в Spring Event система
     */
    private void publishEvent(TickerDto ticker) {
        if (ticker.getSymbol() != null && ticker.getLastPrice() != null) {
            MarketDataEvent event = new MarketDataEvent(this, ticker);
            eventPublisher.publishEvent(event);
        } else {
            log.warn("Skipping event publication due to missing required fields: symbol={}, price={}",
                    ticker.getSymbol(), ticker.getLastPrice());
        }
    }

    /**
     * Конвертирует timestamp из миллисекунд в LocalDateTime (московское время)
     */
    private LocalDateTime convertTimestamp(long epochMilli) {
        return LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(epochMilli),
                ZoneId.of("Europe/Moscow")
        );
    }
}