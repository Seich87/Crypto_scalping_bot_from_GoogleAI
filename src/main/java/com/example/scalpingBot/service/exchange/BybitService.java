package com.example.scalpingBot.service.exchange;

import com.example.scalpingBot.dto.exchange.BalanceDto;
import com.example.scalpingBot.dto.exchange.OrderDto;
import com.example.scalpingBot.dto.exchange.TickerDto;
import com.example.scalpingBot.enums.OrderSide;
import com.example.scalpingBot.enums.OrderStatus;
import com.example.scalpingBot.enums.OrderType;
import com.example.scalpingBot.exception.ExchangeApiException;
import com.example.scalpingBot.utils.CryptoUtils;
import com.example.scalpingBot.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Реализация ExchangeApiService для взаимодействия с биржей Bybit (API V5).
 * Класс является потокобезопасным и готовым к работе.
 */
@Service
@Qualifier("bybit")
public class BybitService implements ExchangeApiService {

    private static final Logger log = LoggerFactory.getLogger(BybitService.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;
    private final String secretKey;
    private final String recvWindow;

    public BybitService(RestTemplate restTemplate,
                        @Value("${bybit.api.base-url}") String baseUrl,
                        @Value("${bybit.api.key}") String apiKey,
                        @Value("${bybit.api.secret}") String secretKey,
                        @Value("${bybit.api.recv-window}") String recvWindow) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.recvWindow = recvWindow;
    }

    @Override
    public OrderDto placeOrder(String symbol, OrderSide side, OrderType type, BigDecimal quantity, BigDecimal price) throws ExchangeApiException {
        String endpoint = "/v5/order/create";
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("category", "spot");
        body.put("symbol", symbol);
        body.put("side", side.name());
        body.put("orderType", type.name()); // Bybit использует camelCase
        body.put("qty", quantity.toPlainString());
        if (type == OrderType.LIMIT) {
            body.put("price", price.toPlainString());
        }

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, createSignedHeaders(null, body));
            ResponseEntity<Map> response = restTemplate.exchange(baseUrl + endpoint, HttpMethod.POST, entity, Map.class);
            Map<String, Object> result = (Map<String, Object>) Objects.requireNonNull(response.getBody()).get("result");
            // Для получения полного статуса ордера, Bybit рекомендует делать доп. запрос
            return getOrderStatus(symbol, (String) result.get("orderId"));
        } catch (HttpClientErrorException e) {
            throw new ExchangeApiException("Bybit | Failed to place order: " + e.getResponseBodyAsString(), e);
        }
    }

    @Override
    public OrderDto getOrderStatus(String symbol, String orderId) throws ExchangeApiException {
        String endpoint = "/v5/order/history";
        String queryString = "category=spot&symbol=" + symbol + "&orderId=" + orderId;
        try {
            HttpEntity<String> entity = new HttpEntity<>(createSignedHeaders(queryString, null));
            ResponseEntity<Map> response = restTemplate.exchange(baseUrl + endpoint + "?" + queryString, HttpMethod.GET, entity, Map.class);
            Map<String, Object> result = (Map<String, Object>) Objects.requireNonNull(response.getBody()).get("result");
            List<Map<String, Object>> orderList = (List<Map<String, Object>>) result.get("list");

            if (orderList == null || orderList.isEmpty()) {
                throw new ExchangeApiException("Bybit | Order not found with ID: " + orderId);
            }
            return mapToOrderDto(orderList.get(0));
        } catch (HttpClientErrorException e) {
            throw new ExchangeApiException("Bybit | Failed to get order status: " + e.getResponseBodyAsString(), e);
        }
    }

    @Override
    public OrderDto cancelOrder(String symbol, String orderId) throws ExchangeApiException {
        String endpoint = "/v5/order/cancel";
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("category", "spot");
        body.put("symbol", symbol);
        body.put("orderId", orderId);
        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, createSignedHeaders(null, body));
            restTemplate.exchange(baseUrl + endpoint, HttpMethod.POST, entity, Map.class);
            // Возвращаем финальный статус, делая доп. запрос
            return getOrderStatus(symbol, orderId);
        } catch (HttpClientErrorException e) {
            throw new ExchangeApiException("Bybit | Failed to cancel order: " + e.getResponseBodyAsString(), e);
        }
    }

    @Override
    public TickerDto getTicker(String symbol) throws ExchangeApiException {
        String endpoint = "/v5/market/tickers?category=spot&symbol=" + symbol;
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(baseUrl + endpoint, Map.class);
            Map<String, Object> result = (Map<String, Object>) Objects.requireNonNull(response.getBody()).get("result");
            List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("list");

            if (list == null || list.isEmpty()) {
                throw new ExchangeApiException("Bybit | Ticker not found for symbol: " + symbol);
            }
            return mapToTickerDto(list.get(0));
        } catch (HttpClientErrorException e) {
            throw new ExchangeApiException("Bybit | Failed to get ticker: " + e.getResponseBodyAsString(), e);
        }
    }

    @Override
    public List<BalanceDto> getBalances() throws ExchangeApiException {
        String endpoint = "/v5/account/wallet-balance";
        String queryString = "accountType=UNIFIED"; // или SPOT для классического спотового аккаунта
        try {
            HttpEntity<String> entity = new HttpEntity<>(createSignedHeaders(queryString, null));
            ResponseEntity<Map> response = restTemplate.exchange(baseUrl + endpoint + "?" + queryString, HttpMethod.GET, entity, Map.class);
            Map<String, Object> result = (Map<String, Object>) Objects.requireNonNull(response.getBody()).get("result");
            List<Map<String, Object>> resultList = (List<Map<String, Object>>) result.get("list");

            if (resultList == null || resultList.isEmpty()) {
                log.warn("Bybit | Account balance list is empty or null.");
                return Collections.emptyList();
            }

            List<Map<String, Object>> coinList = (List<Map<String, Object>>) resultList.get(0).get("coin");
            if (coinList == null) {
                return Collections.emptyList();
            }

            return coinList.stream().map(this::mapToBalanceDto).collect(Collectors.toList());
        } catch (HttpClientErrorException e) {
            throw new ExchangeApiException("Bybit | Failed to get balances: " + e.getResponseBodyAsString(), e);
        }
    }

    @Override
    public long getServerTime() throws ExchangeApiException {
        String endpoint = "/v5/market/time";
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(baseUrl + endpoint, Map.class);
            Map<String, Object> result = (Map<String, Object>) Objects.requireNonNull(response.getBody()).get("result");
            String timeMilli = (String) result.get("timeMilli");
            return Long.parseLong(timeMilli);
        } catch (HttpClientErrorException e) {
            throw new ExchangeApiException("Bybit | Failed to get server time: " + e.getResponseBodyAsString(), e);
        }
    }

    private HttpHeaders createSignedHeaders(String queryString, Map<String, Object> body) {
        String timestamp = Long.toString(System.currentTimeMillis());
        String bodyString = "";
        if (body != null && !body.isEmpty()) {
            bodyString = body.entrySet().stream()
                    .map(e -> "\"" + e.getKey() + "\":\"" + e.getValue().toString() + "\"")
                    .collect(Collectors.joining(",", "{", "}"));
        }

        String dataToSign = timestamp + apiKey + recvWindow + (queryString != null ? queryString : "") + bodyString;
        String signature = CryptoUtils.generateHmacSha256(dataToSign, secretKey);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-BAPI-API-KEY", apiKey);
        headers.set("X-BAPI-TIMESTAMP", timestamp);
        headers.set("X-BAPI-SIGN", signature);
        headers.set("X-BAPI-RECV-WINDOW", recvWindow);
        if (body != null && !body.isEmpty()) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        return headers;
    }

    private TickerDto mapToTickerDto(Map<String, Object> data) {
        return TickerDto.builder()
                .symbol((String) data.get("symbol"))
                .lastPrice(new BigDecimal((String) data.get("lastPrice")))
                .bestBidPrice(new BigDecimal((String) data.get("bid1Price")))
                .bestAskPrice(new BigDecimal((String) data.get("ask1Price")))
                .volume24h(new BigDecimal((String) data.get("volume24h")))
                .quoteVolume24h(new BigDecimal((String) data.get("turnover24h")))
                .priceChangePercent24h(new BigDecimal((String) data.get("price24hPcnt")).multiply(new BigDecimal(100)))
                .timestamp(DateUtils.nowInMoscow())
                .build();
    }

    private BalanceDto mapToBalanceDto(Map<String, Object> data) {
        BigDecimal total = new BigDecimal((String) data.get("walletBalance"));
        // Bybit может не возвращать это поле, если оно равно 0
        String availableStr = (String) data.getOrDefault("availableToWithdraw", "0");
        BigDecimal available = new BigDecimal(availableStr);
        return BalanceDto.builder()
                .asset((String) data.get("coin"))
                .free(available)
                .locked(total.subtract(available))
                .build();
    }

    private OrderDto mapToOrderDto(Map<String, Object> data) {
        return OrderDto.builder()
                .exchangeOrderId((String) data.get("orderId"))
                .clientOrderId((String) data.get("orderLinkId"))
                .symbol((String) data.get("symbol"))
                .side(OrderSide.valueOf((String) data.get("side")))
                .type(OrderType.valueOf(((String) data.get("orderType")).toUpperCase()))
                .status(mapOrderStatus((String) data.get("orderStatus")))
                .price(new BigDecimal((String) data.get("price")))
                .originalQuantity(new BigDecimal((String) data.get("qty")))
                .executedQuantity(new BigDecimal((String) data.get("cumExecQty")))
                .cumulativeQuoteQuantity(new BigDecimal((String) data.get("cumExecValue")))
                .createdAt(DateUtils.fromUnixMillis(Long.parseLong((String) data.get("createdTime"))))
                .updatedAt(DateUtils.fromUnixMillis(Long.parseLong((String) data.get("updatedTime"))))
                .build();
    }

    private OrderStatus mapOrderStatus(String bybitStatus) {
        return switch (bybitStatus) {
            case "New", "Untriggered" -> OrderStatus.NEW;
            case "PartiallyFilled" -> OrderStatus.PARTIALLY_FILLED;
            case "Filled" -> OrderStatus.FILLED;
            case "Cancelled", "Deactivated" -> OrderStatus.CANCELED;
            case "Rejected" -> OrderStatus.REJECTED;
            default -> {
                log.warn("Unknown Bybit order status received: {}", bybitStatus);
                yield OrderStatus.REJECTED;
            }
        };
    }
}