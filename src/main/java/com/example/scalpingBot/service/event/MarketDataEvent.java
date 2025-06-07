package com.example.scalpingBot.service.event;

import com.example.scalpingBot.dto.exchange.TickerDto;
import org.springframework.context.ApplicationEvent;

public class MarketDataEvent extends ApplicationEvent {
    private final TickerDto ticker;

    public MarketDataEvent(Object source, TickerDto ticker) {
        super(source);
        this.ticker = ticker;
    }

    public TickerDto getTicker() {
        return ticker;
    }
}