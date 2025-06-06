package com.example.scalpingBot.service.exchange;

import com.example.scalpingBot.dto.exchange.BalanceDto;
import com.example.scalpingBot.dto.exchange.OrderDto;
import com.example.scalpingBot.dto.exchange.TickerDto;
import com.example.scalpingBot.enums.OrderSide;
import com.example.scalpingBot.enums.OrderType;
import com.example.scalpingBot.exception.ExchangeApiException;

import java.math.BigDecimal;
import java.util.List;

/**
 * Интерфейс, определяющий стандартный набор функций для взаимодействия с любой криптобиржей.
 * Каждый сервис, поддерживающий конкретную биржу (например, BinanceService), должен реализовать этот интерфейс.
 * Это позволяет основной логике бота работать с любой биржей через единый контракт.
 */
public interface ExchangeApiService {

    /**
     * Размещает новый ордер на бирже.
     *
     * @param symbol   Торговая пара (например, "BTCUSDT").
     * @param side     Сторона ордера (BUY или SELL).
     * @param type     Тип ордера (MARKET или LIMIT).
     * @param quantity Количество для покупки/продажи.
     * @param price    Цена для лимитного ордера (может быть null для рыночного).
     * @return OrderDto с информацией о размещенном ордере.
     * @throws ExchangeApiException в случае ошибки со стороны биржи.
     */
    OrderDto placeOrder(String symbol, OrderSide side, OrderType type, BigDecimal quantity, BigDecimal price) throws ExchangeApiException;

    /**
     * Получает текущий статус ордера по его ID.
     *
     * @param symbol  Торговая пара.
     * @param orderId Уникальный идентификатор ордера на бирже.
     * @return OrderDto с обновленной информацией об ордере.
     * @throws ExchangeApiException в случае ошибки со стороны биржи.
     */
    OrderDto getOrderStatus(String symbol, String orderId) throws ExchangeApiException;

    /**
     * Отменяет активный ордер.
     *
     * @param symbol  Торговая пара.
     * @param orderId Уникальный идентификатор ордера на бирже.
     * @return OrderDto с финальным статусом ордера (CANCELED).
     * @throws ExchangeApiException в случае ошибки со стороны биржи.
     */
    OrderDto cancelOrder(String symbol, String orderId) throws ExchangeApiException;

    /**
     * Получает последние рыночные данные (тикер) для одной или нескольких торговых пар.
     *
     * @param symbol Торговая пара.
     * @return TickerDto с последними данными о цене, объеме и т.д.
     * @throws ExchangeApiException в случае ошибки со стороны биржи.
     */
    TickerDto getTicker(String symbol) throws ExchangeApiException;

    /**
     * Получает балансы всех активов на счете.
     *
     * @return Список BalanceDto, представляющих баланс каждого актива.
     * @throws ExchangeApiException в случае ошибки со стороны биржи.
     */
    List<BalanceDto> getBalances() throws ExchangeApiException;

    /**
     * Получает текущее время сервера биржи в формате Unix timestamp (миллисекунды).
     * Необходимо для синхронизации и создания подписи запросов.
     *
     * @return Время сервера в миллисекундах.
     * @throws ExchangeApiException в случае ошибки со стороны биржи.
     */
    long getServerTime() throws ExchangeApiException;
}