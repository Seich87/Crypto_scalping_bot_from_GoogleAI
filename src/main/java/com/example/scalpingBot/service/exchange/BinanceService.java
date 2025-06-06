package com.example.scalpingBot.service.exchange;

import com.example.scalpingBot.dto.exchange.BalanceDto;
import com.example.scalpingBot.dto.exchange.OrderDto;
import com.example.scalpingBot.dto.exchange.TickerDto;
import com.example.scalpingBot.entity.Position;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Реализация ExchangeApiService для взаимодействия с биржей Binance.
 */
@Service
@Qualifier("binance")
public class BinanceService implements ExchangeApiService {

    private static final Logger log = LoggerFactory.getLogger(BinanceService.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;
    private final String secretKey;
    private final String quoteAsset;

    public BinanceService(RestTemplate restTemplate,
                          @Value("${binance.api.base-url}") String baseUrl,
                          @Value("${binance.api.key}") String apiKey,
                          @Value("${binance.api.secret}") String secretKey,
                          @Value("${risk.quote-asset:USDT}") String quoteAsset) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.quoteAsset = quoteAsset;
    }

    @Override
    public OrderDto placeOrder(String symbol, OrderSide side, OrderType type, BigDecimal quantity, BigDecimal price) throws ExchangeApiException {
        String endpoint = "/api/v3/order";
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(endpoint)
                .queryParam("symbol", symbol)
                .queryParam("side", side.name())
                .queryParam("type", type.name())
                .queryParam("quantity", quantity.toPlainString());

        if (type == OrderType.LIMIT) {
            if (price == null) throw new IllegalArgumentException("Price is required for a LIMIT order.");
            builder.queryParam("price", price.toPlainString())
                    .queryParam("timeInForce", "GTC"); // Good-'Til-Canceled
        }

        String fullUrl = baseUrl + createSignedQueryString(builder.build(true).getQuery());

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    fullUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(createHeaders()),
                    new ParameterizedTypeReference<>() {}
            );
            return mapToOrderDto(Objects.requireNonNull(response.getBody()));
        } catch (HttpClientErrorException e) {
            log.error("Binance API error on placeOrder: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ExchangeApiException("Failed to place order on Binance: " + e.getResponseBodyAsString(), e);
        }
    }

    @Override
    public TickerDto getTicker(String symbol) throws ExchangeApiException {
        String endpoint = "/api/v3/ticker/24hr";
        String url = baseUrl + endpoint + "?symbol=" + symbol;
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return mapToTickerDto(Objects.requireNonNull(response.getBody()));
        } catch (HttpClientErrorException e) {
            log.error("Binance API error on getTicker for {}: {} - {}", symbol, e.getStatusCode(), e.getResponseBodyAsString());
            throw new ExchangeApiException("Failed to get ticker for " + symbol + ": " + e.getResponseBodyAsString(), e);
        }
    }

    @Override
    public List<BalanceDto> getBalances() throws ExchangeApiException {
        String endpoint = "/api/v3/account";
        String fullUrl = baseUrl + createSignedQueryString("");

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    fullUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(createHeaders()),
                    new ParameterizedTypeReference<>() {}
            );

            List<Map<String, String>> balances = (List<Map<String, String>>) Objects.requireNonNull(response.getBody()).get("balances");
            return balances.stream()
                    .map(this::mapToBalanceDto)
                    .filter(b -> b.getFree().compareTo(BigDecimal.ZERO) > 0 || b.getLocked().compareTo(BigDecimal.ZERO) > 0)
                    .collect(Collectors.toList());
        } catch (HttpClientErrorException e) {
            log.error("Binance API error on getBalances: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ExchangeApiException("Failed to get account balances: " + e.getResponseBodyAsString(), e);
        }
    }

    @Override
    public OrderDto getOrderStatus(String symbol, String orderId) throws ExchangeApiException {
        String endpoint = "/api/v3/order";
        String query = "symbol=" + symbol + "&orderId=" + orderId;
        String fullUrl = baseUrl + createSignedQueryString(query);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    fullUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(createHeaders()),
                    new ParameterizedTypeReference<>() {}
            );
            return mapToOrderDto(Objects.requireNonNull(response.getBody()));
        } catch (HttpClientErrorException e) {
            log.error("Binance API error on getOrderStatus for order {}: {} - {}", orderId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new ExchangeApiException("Failed to get order status: " + e.getResponseBodyAsString(), e);
        }
    }

    @Override
    public OrderDto cancelOrder(String symbol, String orderId) throws ExchangeApiException {
        String endpoint = "/api/v3/order";
        String query = "symbol=" + symbol + "&orderId=" + orderId;
        String fullUrl = baseUrl + createSignedQueryString(query);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    fullUrl,
                    HttpMethod.DELETE,
                    new HttpEntity<>(createHeaders()),
                    new ParameterizedTypeReference<>() {}
            );
            return mapToOrderDto(Objects.requireNonNull(response.getBody()));
        } catch (HttpClientErrorException e) {
            log.error("Binance API error on cancelOrder for order {}: {} - {}", orderId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new ExchangeApiException("Failed to cancel order: " + e.getResponseBodyAsString(), e);
        }
    }

    @Override
    public long getServerTime() throws ExchangeApiException {
        String endpoint = "/api/v3/time";
        String url = baseUrl + endpoint;
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return Long.parseLong(Objects.requireNonNull(response.getBody()).get("serverTime").toString());
        } catch (HttpClientErrorException e) {
            log.error("Binance API error on getServerTime: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ExchangeApiException("Failed to get server time: " + e.getResponseBodyAsString(), e);
        }
    }

    @Override
    public List<OrderDto> getOpenOrders(String symbol) throws ExchangeApiException {
        String endpoint = "/api/v3/openOrders";
        String query = "symbol=" + symbol;
        String fullUrl = baseUrl + createSignedQueryString(query);

        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    fullUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(createHeaders()),
                    new ParameterizedTypeReference<>() {}
            );
            return Objects.requireNonNull(response.getBody()).stream()
                    .map(this::mapToOrderDto)
                    .collect(Collectors.toList());
        } catch (HttpClientErrorException e) {
            throw new ExchangeApiException("Binance | Failed to get open orders: " + e.getResponseBodyAsString(), e);
        }
    }

    @Override
    public Optional<Position> getExchangePosition(String symbol) throws ExchangeApiException {
        String baseAsset = symbol.replace(this.quoteAsset, "");

        return getBalances().stream()
                .filter(balance -> balance.getAsset().equalsIgnoreCase(baseAsset))
                .filter(balance -> balance.getFree().add(balance.getLocked()).compareTo(new BigDecimal("0.00001")) > 0)
                .map(balance -> Position.builder()
                        .tradingPair(symbol)
                        .quantity(balance.getFree().add(balance.getLocked()))
                        .isActive(true)
                        .build())
                .findFirst();
    }


    private String createSignedQueryString(String query) {
        long timestamp = System.currentTimeMillis();
        String queryStringWithTimestamp = query + (query.isEmpty() ? "" : "&") + "timestamp=" + timestamp;
        String signature = CryptoUtils.generateHmacSha256(queryStringWithTimestamp, secretKey);
        return UriComponentsBuilder.fromPath("")
                .query(queryStringWithTimestamp)
                .queryParam("signature", signature)
                .build(true)
                .toUriString();
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-MBX-APIKEY", apiKey);
        return headers;
    }

    private OrderDto mapToOrderDto(Map<String, Object> response) {
        return OrderDto.builder()
                .exchangeOrderId(response.get("orderId").toString())
                .clientOrderId((String) response.get("clientOrderId"))
                .symbol(response.get("symbol").toString())
                .side(OrderSide.valueOf(response.get("side").toString()))
                .type(OrderType.valueOf(response.get("type").toString()))
                .status(OrderStatus.valueOf(response.get("status").toString()))
                .price(new BigDecimal(response.get("price").toString()))
                .originalQuantity(new BigDecimal(response.get("origQty").toString()))
                .executedQuantity(new BigDecimal(response.get("executedQty").toString()))
                .cumulativeQuoteQuantity(new BigDecimal(response.get("cummulativeQuoteQty").toString()))
                .createdAt(DateUtils.fromUnixMillis(Long.parseLong(response.get("time").toString())))
                .updatedAt(DateUtils.fromUnixMillis(Long.parseLong(response.get("updateTime").toString())))
                .build();
    }

    private TickerDto mapToTickerDto(Map<String, Object> response) {
        return TickerDto.builder()
                .symbol(response.get("symbol").toString())
                .lastPrice(new BigDecimal(response.get("lastPrice").toString()))
                .bestBidPrice(new BigDecimal(response.get("bidPrice").toString()))
                .bestAskPrice(new BigDecimal(response.get("askPrice").toString()))
                .volume24h(new BigDecimal(response.get("volume").toString()))
                .quoteVolume24h(new BigDecimal(response.get("quoteVolume").toString()))
                .priceChangePercent24h(new BigDecimal(response.get("priceChangePercent").toString()))
                .timestamp(DateUtils.nowInMoscow())
                .build();
    }

    private BalanceDto mapToBalanceDto(Map<String, String> balance) {
        return BalanceDto.builder()
                .asset(balance.get("asset"))
                .free(new BigDecimal(balance.get("free")))
                .locked(new BigDecimal(balance.get("locked")))
                .build();
    }
}