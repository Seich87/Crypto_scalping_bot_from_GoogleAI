package com.example.scalpingBot.service.event;

import com.example.scalpingBot.dto.exchange.TickerDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MarketDataEventPublisher {

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MarketDataEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishTickerEvent(String jsonMessage) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonMessage);
            JsonNode dataNode = rootNode.path("data");

            // Парсим сообщение от Binance
            String symbol = dataNode.path("s").asText();
            BigDecimal price = new BigDecimal(dataNode.path("p").asText());

            // Создаем наш унифицированный TickerDto (можно заполнить и остальные поля)
            TickerDto ticker = TickerDto.builder()
                    .symbol(symbol)
                    .lastPrice(price)
                    .build();

            MarketDataEvent event = new MarketDataEvent(this, ticker);
            eventPublisher.publishEvent(event);

        } catch (JsonProcessingException e) {
            // Игнорируем некорректные сообщения
        }
    }
}