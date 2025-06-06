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
 * Сервис, отвечающий за мониторинг и срабатывание стоп-лоссов.
 * Постоянно проверяет, не достигла ли цена уровня стоп-лосса для активных позиций.
 */
@Service
public class StopLossService {

    private static final Logger log = LoggerFactory.getLogger(StopLossService.class);

    private final PositionManager positionManager;
    private final MarketDataService marketDataService;

    @Autowired
    public StopLossService(PositionManager positionManager, MarketDataService marketDataService) {
        this.positionManager = positionManager;
        this.marketDataService = marketDataService;
    }

    /**
     * Проверяет и при необходимости инициирует закрытие позиции по стоп-лоссу.
     * Этот метод должен вызываться регулярно (например, каждую секунду) планировщиком.
     *
     * @param position Активная позиция для проверки.
     */
    public void checkAndTriggerStopLoss(Position position) {
        if (position == null || !position.isActive()) {
            return; // Проверяем только активные позиции
        }

        BigDecimal stopLossPrice = position.getStopLossPrice();
        if (stopLossPrice == null) {
            log.warn("Position {} for {} has no stop-loss price set.", position.getId(), position.getTradingPair());
            return;
        }

        try {
            // 1. Получаем актуальную цену с биржи
            TickerDto currentTicker = marketDataService.getCurrentTicker(position.getTradingPair());
            BigDecimal currentPrice = currentTicker.getLastPrice();

            // 2. Проверяем условие срабатывания стоп-лосса
            boolean shouldTrigger = false;
            if (position.getSide() == OrderSide.BUY) {
                // Для LONG-позиции стоп-лосс срабатывает, если цена упала НИЖЕ или РАВНО SL
                shouldTrigger = currentPrice.compareTo(stopLossPrice) <= 0;
            } else { // OrderSide.SELL
                // Для SHORT-позиции стоп-лосс срабатывает, если цена выросла ВЫШЕ или РАВНО SL
                shouldTrigger = currentPrice.compareTo(stopLossPrice) >= 0;
            }

            // 3. Если условие выполнено, закрываем позицию
            if (shouldTrigger) {
                log.warn("!!! STOP-LOSS TRIGGERED for {} !!! Current price: {}, SL price: {}. Closing position.",
                        position.getTradingPair(), currentPrice, stopLossPrice);
                // Делегируем закрытие PositionManager'у
                positionManager.closePosition(position.getTradingPair(), currentPrice);
            }

        } catch (Exception e) {
            // Логируем ошибку, но не останавливаем работу.
            // Следующая проверка может быть успешной.
            log.error("Error during stop-loss check for {}: {}", position.getTradingPair(), e.getMessage());
        }
    }
}