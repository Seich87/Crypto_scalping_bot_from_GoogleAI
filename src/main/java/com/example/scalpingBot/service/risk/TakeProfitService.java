package com.example.scalpingBot.service.risk;

import com.example.scalpingBot.dto.exchange.TickerDto;
import com.example.scalpingBot.entity.Position;
import com.example.scalpingBot.enums.OrderSide;
import com.example.scalpingBot.service.market.MarketDataService;
import com.example.scalpingBot.service.trading.PositionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Сервис, отвечающий за мониторинг и срабатывание тейк-профитов.
 * Постоянно проверяет, не достигла ли цена уровня тейк-профита для активных позиций.
 */
@Service
public class TakeProfitService {

    private static final Logger log = LoggerFactory.getLogger(TakeProfitService.class);

    private final PositionManager positionManager;
    private final MarketDataService marketDataService;

    @Autowired
    public TakeProfitService(PositionManager positionManager, MarketDataService marketDataService) {
        this.positionManager = positionManager;
        this.marketDataService = marketDataService;
    }

    /**
     * Проверяет и при необходимости инициирует закрытие позиции по тейк-профиту.
     * Этот метод должен вызываться регулярно планировщиком.
     *
     * @param position Активная позиция для проверки.
     */
    public void checkAndTriggerTakeProfit(Position position) {
        if (position == null || !position.isActive()) {
            return; // Проверяем только активные позиции
        }

        BigDecimal takeProfitPrice = position.getTakeProfitPrice();
        if (takeProfitPrice == null) {
            log.warn("Position {} for {} has no take-profit price set.", position.getId(), position.getTradingPair());
            return;
        }

        try {
            // 1. Получаем актуальную цену с биржи
            TickerDto currentTicker = marketDataService.getCurrentTicker(position.getTradingPair());
            BigDecimal currentPrice = currentTicker.getLastPrice();

            // 2. Проверяем условие срабатывания тейк-профита
            boolean shouldTrigger = false;
            if (position.getSide() == OrderSide.BUY) {
                // Для LONG-позиции тейк-профит срабатывает, если цена выросла ВЫШЕ или РАВНО TP
                shouldTrigger = currentPrice.compareTo(takeProfitPrice) >= 0;
            } else { // OrderSide.SELL
                // Для SHORT-позиции тейк-профит срабатывает, если цена упала НИЖЕ или РАВНО TP
                shouldTrigger = currentPrice.compareTo(takeProfitPrice) <= 0;
            }

            // 3. Если условие выполнено, закрываем позицию
            if (shouldTrigger) {
                log.info("$$$ TAKE-PROFIT TRIGGERED for {} $$$ Current price: {}, TP price: {}. Closing position.",
                        position.getTradingPair(), currentPrice, takeProfitPrice);
                // Делегируем закрытие PositionManager'у
                positionManager.closePosition(position.getTradingPair(), currentPrice);
            }

        } catch (Exception e) {
            log.error("Error during take-profit check for {}: {}", position.getTradingPair(), e.getMessage());
        }
    }
}