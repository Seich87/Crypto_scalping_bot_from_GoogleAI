package com.example.scalpingBot.service.trading;

import com.example.scalpingBot.dto.exchange.TickerDto;
import com.example.scalpingBot.dto.response.PositionResponse;
import com.example.scalpingBot.dto.response.TradeResponse;
import com.example.scalpingBot.entity.Position;
import com.example.scalpingBot.entity.Trade;
import com.example.scalpingBot.repository.PositionRepository;
import com.example.scalpingBot.repository.TradeRepository;
import com.example.scalpingBot.service.market.MarketDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис, отвечающий за выполнение запросов на чтение (Query)
 * торговых данных и их преобразование в DTO.
 */
@Service
@Transactional(readOnly = true) // Весь сервис работает только на чтение
public class TradingQueryService {

    private final PositionRepository positionRepository;
    private final TradeRepository tradeRepository;
    private final MarketDataService marketDataService;

    @Autowired
    public TradingQueryService(PositionRepository positionRepository,
                               TradeRepository tradeRepository,
                               MarketDataService marketDataService) {
        this.positionRepository = positionRepository;
        this.tradeRepository = tradeRepository;
        this.marketDataService = marketDataService;
    }

    public List<PositionResponse> getActivePositions() {
        List<Position> activePositions = positionRepository.findAllByIsActive(true);
        if (activePositions.isEmpty()) {
            return Collections.emptyList();
        }
        return activePositions.stream()
                .map(this::mapPositionToResponse)
                .collect(Collectors.toList());
    }

    public List<PositionResponse> getPositionHistory(String symbol) {
        List<Position> positionHistory = positionRepository.findAllByTradingPairOrderByCloseTimestampDesc(symbol);
        return positionHistory.stream()
                .map(this::mapPositionToResponse)
                .collect(Collectors.toList());
    }

    public List<TradeResponse> getTradeHistory(String symbol) {
        List<Trade> trades = tradeRepository.findAllByTradingPairOrderByExecutionTimestampDesc(symbol);
        return trades.stream()
                .map(this::mapTradeToResponse)
                .collect(Collectors.toList());
    }

    private PositionResponse mapPositionToResponse(Position position) {
        PositionResponse.PositionResponseBuilder builder = PositionResponse.builder()
                .tradingPair(position.getTradingPair())
                .side(position.getSide())
                .quantity(position.getQuantity())
                .entryPrice(position.getEntryPrice())
                .stopLossPrice(position.getStopLossPrice())
                .takeProfitPrice(position.getTakeProfitPrice())
                .isActive(position.isActive())
                .realizedPnl(position.getPnl())
                .openTimestamp(position.getOpenTimestamp())
                .closeTimestamp(position.getCloseTimestamp());

        // Для активных позиций дополнительно рассчитываем текущую цену и нереализованный PnL
        if (position.isActive()) {
            try {
                TickerDto ticker = marketDataService.getCurrentTicker(position.getTradingPair());
                BigDecimal currentPrice = ticker.getLastPrice();
                BigDecimal pnl = (currentPrice.subtract(position.getEntryPrice())).multiply(position.getQuantity());
                if (position.getSide() == com.example.scalpingBot.enums.OrderSide.SELL) {
                    pnl = pnl.negate();
                }
                builder.currentPrice(currentPrice).unrealizedPnl(pnl);
            } catch (Exception e) {
                // Если не удалось получить цену, оставляем поля пустыми
            }
        }
        return builder.build();
    }

    private TradeResponse mapTradeToResponse(Trade trade) {
        return TradeResponse.builder()
                .exchangeTradeId(trade.getExchangeTradeId())
                .tradingPair(trade.getTradingPair())
                .status(trade.getStatus())
                .side(trade.getSide())
                .type(trade.getType())
                .price(trade.getPrice())
                .quantity(trade.getQuantity())
                .commission(trade.getCommission())
                .commissionAsset(trade.getCommissionAsset())
                .executionTimestamp(trade.getExecutionTimestamp())
                .build();
    }
}