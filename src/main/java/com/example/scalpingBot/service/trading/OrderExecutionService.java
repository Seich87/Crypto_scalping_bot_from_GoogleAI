package com.example.scalpingBot.service.trading;

import com.example.scalpingBot.dto.exchange.OrderDto;
import com.example.scalpingBot.enums.OrderSide;
import com.example.scalpingBot.enums.OrderType;
import com.example.scalpingBot.exception.ExchangeApiException;
import com.example.scalpingBot.exception.TradingException;
import com.example.scalpingBot.service.exchange.ExchangeApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Сервис, отвечающий за исполнение ордеров на бирже.
 * Он принимает решение о типе и параметрах ордера и отправляет его через ExchangeApiService.
 */
@Service
public class OrderExecutionService {

    private static final Logger log = LoggerFactory.getLogger(OrderExecutionService.class);

    private final ExchangeApiService exchangeApiService;

    @Autowired
    public OrderExecutionService(@Qualifier("binance") ExchangeApiService exchangeApiService) {
        this.exchangeApiService = exchangeApiService;
    }

    /**
     * Исполняет торговую операцию.
     * На данный момент использует простой рыночный ордер (MARKET).
     *
     * @param symbol   Торговая пара.
     * @param side     Сторона сделки (BUY или SELL).
     * @param quantity Количество актива для покупки/продажи.
     * @return OrderDto с информацией о размещенном ордере.
     * @throws TradingException если происходит ошибка в логике исполнения.
     * @throws ExchangeApiException если происходит ошибка при взаимодействии с биржей.
     */
    public OrderDto executeOrder(String symbol, OrderSide side, BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TradingException("Order quantity must be positive.");
        }

        // Для скальпинга часто важна скорость, поэтому по умолчанию используем MARKET ордер.
        // В будущем здесь можно добавить логику для выбора между MARKET и LIMIT ордерами.
        OrderType orderType = OrderType.MARKET;
        BigDecimal price = null; // Для MARKET ордера цена не указывается.

        log.info("Executing {} {} order for {} of {}", orderType, side, quantity.toPlainString(), symbol);

        try {
            OrderDto placedOrder = exchangeApiService.placeOrder(symbol, side, orderType, quantity, price);
            log.info("Successfully placed order for {}. Exchange Order ID: {}", symbol, placedOrder.getExchangeOrderId());
            return placedOrder;
        } catch (ExchangeApiException e) {
            log.error("Failed to execute order for {}. Reason: {}", symbol, e.getMessage());
            // Пробрасываем исключение дальше, чтобы вышестоящий сервис мог его обработать
            throw e;
        }
    }

    /**
     * Отменяет существующий ордер на бирже.
     *
     * @param symbol  Торговая пара.
     * @param orderId ID ордера на бирже.
     * @return OrderDto с финальным статусом ордера.
     */
    public OrderDto cancelOrder(String symbol, String orderId) {
        log.info("Attempting to cancel order {} for {}", orderId, symbol);
        try {
            OrderDto cancelledOrder = exchangeApiService.cancelOrder(symbol, orderId);
            log.info("Successfully cancelled order {}. Current status: {}", orderId, cancelledOrder.getStatus());
            return cancelledOrder;
        } catch (ExchangeApiException e) {
            log.error("Failed to cancel order {} for {}. Reason: {}", orderId, e.getMessage());
            throw e;
        }
    }
}