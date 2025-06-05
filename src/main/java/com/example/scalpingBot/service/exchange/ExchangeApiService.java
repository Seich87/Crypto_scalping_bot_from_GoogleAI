package com.example.scalpingBot.service.exchange;

import com.example.scalpingBot.exception.ExchangeApiException;
import com.example.scalpingBot.utils.CryptoUtils;
import com.example.scalpingBot.utils.DateUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис для взаимодействия с API криптовалютных бирж
 *
 * Поддерживаемые биржи:
 * - Binance (основная для скальпинга)
 * - Bybit (резервная)
 *
 * Основные функции:
 * - Размещение и отмена ордеров
 * - Получение рыночных данных в реальном времени
 * - Управление балансом и позициями
 * - Подписка на WebSocket потоки
 * - Обработка rate limits и ошибок API
 * - Автоматическая аутентификация и подписи запросов
 *
 * Все операции оптимизированы для скальпинг-стратегии
 * с минимальными задержками.
 *
 * @author ScalpingBot Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "exchanges")
public class ExchangeApiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Конфигурация бирж
     */
    @Value("${exchanges.binance.api-key:}")
    private String binanceApiKey;

    @Value("${exchanges.binance.secret-key:}")
    private String binanceSecretKey;

    @Value("${exchanges.binance.api-url:https://api.binance.com}")
    private String binanceApiUrl;

    @Value("${exchanges.binance.testnet:true}")
    private boolean binanceTestnet;

    @Value("${exchanges.bybit.api-key:}")
    private String bybitApiKey;

    @Value("${exchanges.bybit.secret-key:}")
    private String bybitSecretKey;

    @Value("${exchanges.bybit.api-url:https://api.bybit.com}")
    private String bybitApiUrl;

    @Value("${exchanges.bybit.testnet:true}")
    private boolean bybitTestnet;

    /**
     * Кеш для rate limits
     */
    private final Map<String, RateLimitInfo> rateLimits = new ConcurrentHashMap<>();

    /**
     * Константы API
     */
    private static final int REQUEST_TIMEOUT_MS = 10000;
    private static final int MAX_RETRIES = 3;
    private static final String BINANCE_RECV_WINDOW = "5000";

    /**
     * Инициализация сервиса
     */
    @PostConstruct
    public void init() {
        log.info("Initializing Exchange API Service");

        // Настройка таймаутов RestTemplate
        restTemplate.getRequestFactory();

        // Проверка конфигурации
        validateConfiguration();

        log.info("Exchange API Service initialized successfully");
        log.info("Binance: {} (testnet: {})", binanceApiUrl, binanceTestnet);
        log.info("Bybit: {} (testnet: {})", bybitApiUrl, bybitTestnet);
    }

    /**
     * Валидация конфигурации API ключей
     */
    private void validateConfiguration() {
        if (binanceApiKey == null || binanceApiKey.isEmpty()) {
            log.warn("Binance API key not configured");
        } else {
            log.info("Binance API key configured: {}", CryptoUtils.maskApiKey(binanceApiKey));
        }

        if (bybitApiKey == null || bybitApiKey.isEmpty()) {
            log.warn("Bybit API key not configured");
        } else {
            log.info("Bybit API key configured: {}", CryptoUtils.maskApiKey(bybitApiKey));
        }
    }

    // === Универсальные методы ===

    /**
     * Получить тикер по торговой паре
     *
     * @param symbol торговая пара
     * @param exchange название биржи
     * @return данные тикера
     */
    public Map<String, Object> getTicker(String symbol, String exchange) {
        switch (exchange.toLowerCase()) {
            case "binance":
                return getBinanceTicker(symbol);
            case "bybit":
                return getBybitTicker(symbol);
            default:
                throw new ExchangeApiException(exchange, ExchangeApiException.ApiErrorType.UNKNOWN_ERROR,
                        "Unsupported exchange: " + exchange, 400);
        }
    }

    /**
     * Получить стакан заявок
     *
     * @param symbol торговая пара
     * @param exchange название биржи
     * @param limit количество уровней
     * @return данные стакана
     */
    public Map<String, Object> getOrderBook(String symbol, String exchange, int limit) {
        switch (exchange.toLowerCase()) {
            case "binance":
                return getBinanceOrderBook(symbol, limit);
            case "bybit":
                return getBybitOrderBook(symbol, limit);
            default:
                throw new ExchangeApiException(exchange, ExchangeApiException.ApiErrorType.UNKNOWN_ERROR,
                        "Unsupported exchange: " + exchange, 400);
        }
    }

    /**
     * Получить свечи
     *
     * @param symbol торговая пара
     * @param interval интервал
     * @param limit количество свечей
     * @param exchange название биржи
     * @return данные свечей
     */
    public List<Map<String, Object>> getKlines(String symbol, String interval, int limit, String exchange) {
        switch (exchange.toLowerCase()) {
            case "binance":
                return getBinanceKlines(symbol, interval, limit);
            case "bybit":
                return getBybitKlines(symbol, interval, limit);
            default:
                throw new ExchangeApiException(exchange, ExchangeApiException.ApiErrorType.UNKNOWN_ERROR,
                        "Unsupported exchange: " + exchange, 400);
        }
    }

    /**
     * Разместить рыночный ордер
     *
     * @param symbol торговая пара
     * @param side сторона (BUY/SELL)
     * @param quantity количество
     * @param exchange название биржи
     * @return результат размещения
     */
    public Map<String, Object> placeMarketOrder(String symbol, String side, BigDecimal quantity, String exchange) {
        switch (exchange.toLowerCase()) {
            case "binance":
                return placeBinanceMarketOrder(symbol, side, quantity);
            case "bybit":
                return placeBybitMarketOrder(symbol, side, quantity);
            default:
                throw new ExchangeApiException(exchange, ExchangeApiException.ApiErrorType.UNKNOWN_ERROR,
                        "Unsupported exchange: " + exchange, 400);
        }
    }

    /**
     * Разместить лимитный ордер
     *
     * @param symbol торговая пара
     * @param side сторона (BUY/SELL)
     * @param quantity количество
     * @param price цена
     * @param exchange название биржи
     * @return результат размещения
     */
    public Map<String, Object> placeLimitOrder(String symbol, String side, BigDecimal quantity, BigDecimal price, String exchange) {
        switch (exchange.toLowerCase()) {
            case "binance":
                return placeBinanceLimitOrder(symbol, side, quantity, price);
            case "bybit":
                return placeBybitLimitOrder(symbol, side, quantity, price);
            default:
                throw new ExchangeApiException(exchange, ExchangeApiException.ApiErrorType.UNKNOWN_ERROR,
                        "Unsupported exchange: " + exchange, 400);
        }
    }

    /**
     * Разместить стоп ордер
     *
     * @param symbol торговая пара
     * @param side сторона (BUY/SELL)
     * @param quantity количество
     * @param stopPrice стоп цена
     * @param exchange название биржи
     * @return результат размещения
     */
    public Map<String, Object> placeStopOrder(String symbol, String side, BigDecimal quantity, BigDecimal stopPrice, String exchange) {
        switch (exchange.toLowerCase()) {
            case "binance":
                return placeBinanceStopOrder(symbol, side, quantity, stopPrice);
            case "bybit":
                return placeBybitStopOrder(symbol, side, quantity, stopPrice);
            default:
                throw new ExchangeApiException(exchange, ExchangeApiException.ApiErrorType.UNKNOWN_ERROR,
                        "Unsupported exchange: " + exchange, 400);
        }
    }

    /**
     * Отменить ордер
     *
     * @param symbol торговая пара
     * @param orderId ID ордера
     * @param exchange название биржи
     * @return результат отмены
     */
    public Map<String, Object> cancelOrder(String symbol, String orderId, String exchange) {
        switch (exchange.toLowerCase()) {
            case "binance":
                return cancelBinanceOrder(symbol, orderId);
            case "bybit":
                return cancelBybitOrder(symbol, orderId);
            default:
                throw new ExchangeApiException(exchange, ExchangeApiException.ApiErrorType.UNKNOWN_ERROR,
                        "Unsupported exchange: " + exchange, 400);
        }
    }

    /**
     * Получить статус ордера
     *
     * @param symbol торговая пара
     * @param orderId ID ордера
     * @param exchange название биржи
     * @return статус ордера
     */
    public Map<String, Object> getOrderStatus(String symbol, String orderId, String exchange) {
        switch (exchange.toLowerCase()) {
            case "binance":
                return getBinanceOrderStatus(symbol, orderId);
            case "bybit":
                return getBybitOrderStatus(symbol, orderId);
            default:
                throw new ExchangeApiException(exchange, ExchangeApiException.ApiErrorType.UNKNOWN_ERROR,
                        "Unsupported exchange: " + exchange, 400);
        }
    }

    // === Binance API методы ===

    /**
     * Получить тикер Binance
     */
    private Map<String, Object> getBinanceTicker(String symbol) {
        String endpoint = "/api/v3/ticker/24hr";
        Map<String, String> params = new HashMap<>();
        params.put("symbol", symbol);

        return makeBinanceRequest(endpoint, HttpMethod.GET, params, false);
    }

    /**
     * Получить стакан Binance
     */
    private Map<String, Object> getBinanceOrderBook(String symbol, int limit) {
        String endpoint = "/api/v3/depth";
        Map<String, String> params = new HashMap<>();
        params.put("symbol", symbol);
        params.put("limit", String.valueOf(limit));

        return makeBinanceRequest(endpoint, HttpMethod.GET, params, false);
    }

    /**
     * Получить свечи Binance
     */
    private List<Map<String, Object>> getBinanceKlines(String symbol, String interval, int limit) {
        String endpoint = "/api/v3/klines";
        Map<String, String> params = new HashMap<>();
        params.put("symbol", symbol);
        params.put("interval", interval);
        params.put("limit", String.valueOf(limit));

        try {
            Object result = makeBinanceRequest(endpoint, HttpMethod.GET, params, false);

            if (result instanceof List) {
                @SuppressWarnings("unchecked")
                List<List<Object>> rawKlines = (List<List<Object>>) result;

                return rawKlines.stream()
                        .map(this::convertBinanceKlineToMap)
                        .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
            }

            return new ArrayList<>();

        } catch (Exception e) {
            log.error("Failed to get Binance klines: {}", e.getMessage());
            throw new ExchangeApiException("binance", ExchangeApiException.ApiErrorType.API_ERROR,
                    "Failed to get klines: " + e.getMessage(), e);
        }
    }

    /**
     * Конвертировать свечу Binance в Map
     */
    private Map<String, Object> convertBinanceKlineToMap(List<Object> kline) {
        Map<String, Object> result = new HashMap<>();
        result.put("openTime", Long.parseLong(kline.get(0).toString()));
        result.put("open", kline.get(1).toString());
        result.put("high", kline.get(2).toString());
        result.put("low", kline.get(3).toString());
        result.put("close", kline.get(4).toString());
        result.put("volume", kline.get(5).toString());
        result.put("closeTime", Long.parseLong(kline.get(6).toString()));
        result.put("quoteVolume", kline.get(7).toString());
        result.put("trades", Integer.parseInt(kline.get(8).toString()));
        return result;
    }

    /**
     * Разместить рыночный ордер Binance
     */
    private Map<String, Object> placeBinanceMarketOrder(String symbol, String side, BigDecimal quantity) {
        String endpoint = "/api/v3/order";
        Map<String, String> params = new HashMap<>();
        params.put("symbol", symbol);
        params.put("side", side.toUpperCase());
        params.put("type", "MARKET");
        params.put("quantity", quantity.toString());
        params.put("recvWindow", BINANCE_RECV_WINDOW);
        params.put("timestamp", String.valueOf(DateUtils.currentTimestampMs()));

        return makeBinanceRequest(endpoint, HttpMethod.POST, params, true);
    }

    /**
     * Разместить лимитный ордер Binance
     */
    private Map<String, Object> placeBinanceLimitOrder(String symbol, String side, BigDecimal quantity, BigDecimal price) {
        String endpoint = "/api/v3/order";
        Map<String, String> params = new HashMap<>();
        params.put("symbol", symbol);
        params.put("side", side.toUpperCase());
        params.put("type", "LIMIT");
        params.put("timeInForce", "GTC");
        params.put("quantity", quantity.toString());
        params.put("price", price.toString());
        params.put("recvWindow", BINANCE_RECV_WINDOW);
        params.put("timestamp", String.valueOf(DateUtils.currentTimestampMs()));

        return makeBinanceRequest(endpoint, HttpMethod.POST, params, true);
    }

    /**
     * Разместить стоп ордер Binance
     */
    private Map<String, Object> placeBinanceStopOrder(String symbol, String side, BigDecimal quantity, BigDecimal stopPrice) {
        String endpoint = "/api/v3/order";
        Map<String, String> params = new HashMap<>();
        params.put("symbol", symbol);
        params.put("side", side.toUpperCase());
        params.put("type", "STOP_LOSS_LIMIT");
        params.put("timeInForce", "GTC");
        params.put("quantity", quantity.toString());
        params.put("price", stopPrice.toString());
        params.put("stopPrice", stopPrice.toString());
        params.put("recvWindow", BINANCE_RECV_WINDOW);
        params.put("timestamp", String.valueOf(DateUtils.currentTimestampMs()));

        return makeBinanceRequest(endpoint, HttpMethod.POST, params, true);
    }

    /**
     * Отменить ордер Binance
     */
    private Map<String, Object> cancelBinanceOrder(String symbol, String orderId) {
        String endpoint = "/api/v3/order";
        Map<String, String> params = new HashMap<>();
        params.put("symbol", symbol);
        params.put("orderId", orderId);
        params.put("recvWindow", BINANCE_RECV_WINDOW);
        params.put("timestamp", String.valueOf(DateUtils.currentTimestampMs()));

        return makeBinanceRequest(endpoint, HttpMethod.DELETE, params, true);
    }

    /**
     * Получить статус ордера Binance
     */
    private Map<String, Object> getBinanceOrderStatus(String symbol, String orderId) {
        String endpoint = "/api/v3/order";
        Map<String, String> params = new HashMap<>();
        params.put("symbol", symbol);
        params.put("orderId", orderId);
        params.put("recvWindow", BINANCE_RECV_WINDOW);
        params.put("timestamp", String.valueOf(DateUtils.currentTimestampMs()));

        return makeBinanceRequest(endpoint, HttpMethod.GET, params, true);
    }

    /**
     * Выполнить запрос к Binance API
     */
    private Map<String, Object> makeBinanceRequest(String endpoint, HttpMethod method, Map<String, String> params, boolean signed) {
        try {
            checkRateLimit("binance");

            String baseUrl = binanceTestnet ? "https://testnet.binance.vision" : binanceApiUrl;
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(baseUrl + endpoint);

            // Добавляем параметры в URL
            String queryString = buildQueryString(params);

            // Добавляем подпись если требуется
            if (signed) {
                String signature = CryptoUtils.createBinanceSignature(queryString, binanceSecretKey);
                params.put("signature", signature);
                queryString = buildQueryString(params);
            }

            String url = baseUrl + endpoint + "?" + queryString;

            // Создаем заголовки
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (signed) {
                headers.set("X-MBX-APIKEY", binanceApiKey);
            }

            HttpEntity<String> entity = new HttpEntity<>(headers);

            log.debug("Binance {} request: {}", method, endpoint);

            ResponseEntity<String> response = restTemplate.exchange(url, method, entity, String.class);

            updateRateLimit("binance", response.getHeaders());

            return parseJsonResponse(response.getBody());

        } catch (HttpClientErrorException e) {
            throw handleBinanceApiError(e);
        } catch (ResourceAccessException e) {
            throw ExchangeApiException.connectionTimeout("binance", REQUEST_TIMEOUT_MS, e);
        } catch (Exception e) {
            log.error("Unexpected error in Binance request: {}", e.getMessage());
            throw new ExchangeApiException("binance", ExchangeApiException.ApiErrorType.UNKNOWN_ERROR,
                    "Unexpected error: " + e.getMessage(), e);
        }
    }

    // === Bybit API методы (базовая реализация) ===

    private Map<String, Object> getBybitTicker(String symbol) {
        // Базовая реализация для Bybit
        throw new ExchangeApiException("bybit", ExchangeApiException.ApiErrorType.UNKNOWN_ERROR,
                "Bybit implementation not complete", 501);
    }

    private Map<String, Object> getBybitOrderBook(String symbol, int limit) {
        throw new ExchangeApiException("bybit", ExchangeApiException.ApiErrorType.UNKNOWN_ERROR,
                "Bybit implementation not complete", 501);
    }

    private List<Map<String, Object>> getBybitKlines(String symbol, String interval, int limit) {
        throw new ExchangeApiException("bybit", ExchangeApiException.ApiErrorType.UNKNOWN_ERROR,
                "Bybit implementation not complete", 501);
    }

    private Map<String, Object> placeBybitMarketOrder(String symbol, String side, BigDecimal quantity) {
        throw new ExchangeApiException("bybit", ExchangeApiException.ApiErrorType.UNKNOWN_ERROR,
                "Bybit implementation not complete", 501);
    }

    private Map<String, Object> placeBybitLimitOrder(String symbol, String side, BigDecimal quantity, BigDecimal price) {
        throw new ExchangeApiException("bybit", ExchangeApiException.ApiErrorType.UNKNOWN_ERROR,
                "Bybit implementation not complete", 501);
    }

    private Map<String, Object> placeBybitStopOrder(String symbol, String side, BigDecimal quantity, BigDecimal stopPrice) {
        throw new ExchangeApiException("bybit", ExchangeApiException.ApiErrorType.UNKNOWN_ERROR,
                "Bybit implementation not complete", 501);
    }

    private Map<String, Object> cancelBybitOrder(String symbol, String orderId) {
        throw new ExchangeApiException("bybit", ExchangeApiException.ApiErrorType.UNKNOWN_ERROR,
                "Bybit implementation not complete", 501);
    }

    private Map<String, Object> getBybitOrderStatus(String symbol, String orderId) {
        throw new ExchangeApiException("bybit", ExchangeApiException.ApiErrorType.UNKNOWN_ERROR,
                "Bybit implementation not complete", 501);
    }

    // === Вспомогательные методы ===

    /**
     * Построить строку запроса
     */
    private String buildQueryString(Map<String, String> params) {
        return params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .reduce((a, b) -> a + "&" + b)
                .orElse("");
    }

    /**
     * Парсить JSON ответ
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonResponse(String json) {
        try {
            Object parsed = objectMapper.readValue(json, Object.class);

            if (parsed instanceof Map) {
                return (Map<String, Object>) parsed;
            } else {
                // Для массивов возвращаем первый элемент или пустую карту
                Map<String, Object> result = new HashMap<>();
                result.put("data", parsed);
                return result;
            }

        } catch (Exception e) {
            log.error("Failed to parse JSON response: {}", e.getMessage());
            throw new ExchangeApiException("unknown", ExchangeApiException.ApiErrorType.PARSING_ERROR,
                    "Failed to parse response: " + e.getMessage(), e);
        }
    }

    /**
     * Обработать ошибку Binance API
     */
    private ExchangeApiException handleBinanceApiError(HttpClientErrorException e) {
        int statusCode = e.getStatusCode().value();
        String responseBody = e.getResponseBodyAsString();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> errorResponse = objectMapper.readValue(responseBody, Map.class);

            Integer code = (Integer) errorResponse.get("code");
            String msg = (String) errorResponse.get("msg");

            // Маппинг специфичных ошибок Binance
            ExchangeApiException.ApiErrorType errorType = mapBinanceErrorCode(code, statusCode);

            return new ExchangeApiException("binance", errorType, msg, statusCode,
                    code != null ? code.toString() : null, null, null, null, null, null);

        } catch (Exception parseError) {
            log.error("Failed to parse Binance error response: {}", parseError.getMessage());
            return new ExchangeApiException("binance", ExchangeApiException.ApiErrorType.UNKNOWN_ERROR,
                    "HTTP " + statusCode + ": " + responseBody, statusCode);
        }
    }

    /**
     * Маппинг кодов ошибок Binance
     */
    private ExchangeApiException.ApiErrorType mapBinanceErrorCode(Integer code, int httpStatus) {
        if (code == null) {
            return ExchangeApiException.ApiErrorType.UNKNOWN_ERROR;
        }

        switch (code) {
            case -1021: // Timestamp outside recv window
                return ExchangeApiException.ApiErrorType.BINANCE_TIMESTAMP_ERROR;
            case -1022: // Invalid signature
                return ExchangeApiException.ApiErrorType.INVALID_SIGNATURE;
            case -2010: // Insufficient balance
                return ExchangeApiException.ApiErrorType.INSUFFICIENT_BALANCE;
            case -2011: // Order does not exist
                return ExchangeApiException.ApiErrorType.ORDER_NOT_FOUND;
            case -1003: // Too many requests
                return ExchangeApiException.ApiErrorType.RATE_LIMIT_EXCEEDED;
            case -1013: // Invalid quantity
                return ExchangeApiException.ApiErrorType.INVALID_QUANTITY;
            case -1111: // Precision over maximum
                return ExchangeApiException.ApiErrorType.PRECISION_OVER_MAXIMUM;
            default:
                if (httpStatus == 401) {
                    return ExchangeApiException.ApiErrorType.AUTHENTICATION_FAILED;
                } else if (httpStatus == 429) {
                    return ExchangeApiException.ApiErrorType.RATE_LIMIT_EXCEEDED;
                } else if (httpStatus >= 500) {
                    return ExchangeApiException.ApiErrorType.INTERNAL_SERVER_ERROR;
                } else {
                    return ExchangeApiException.ApiErrorType.UNKNOWN_ERROR;
                }
        }
    }

    /**
     * Проверить rate limit
     */
    private void checkRateLimit(String exchange) {
        RateLimitInfo rateLimit = rateLimits.get(exchange);

        if (rateLimit != null && rateLimit.isLimitExceeded()) {
            long waitTime = rateLimit.getWaitTimeMs();

            if (waitTime > 0) {
                log.warn("Rate limit exceeded for {}, waiting {} ms", exchange, waitTime);

                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ExchangeApiException(exchange, ExchangeApiException.ApiErrorType.RATE_LIMIT_EXCEEDED,
                            "Rate limit wait interrupted", 429);
                }
            }
        }
    }

    /**
     * Обновить информацию о rate limit
     */
    private void updateRateLimit(String exchange, HttpHeaders headers) {
        try {
            String remaining = headers.getFirst("x-mbx-used-weight-1m");
            String limit = headers.getFirst("x-mbx-order-count-10s");

            if (remaining != null || limit != null) {
                RateLimitInfo rateLimit = rateLimits.computeIfAbsent(exchange, k -> new RateLimitInfo());

                if (remaining != null) {
                    rateLimit.setUsedWeight(Integer.parseInt(remaining));
                }

                if (limit != null) {
                    rateLimit.setOrderCount(Integer.parseInt(limit));
                }

                rateLimit.setLastUpdate(System.currentTimeMillis());
            }

        } catch (Exception e) {
            log.debug("Failed to parse rate limit headers: {}", e.getMessage());
        }
    }

    // === Вложенные классы ===

    /**
     * Информация о rate limit
     */
    private static class RateLimitInfo {
        private int usedWeight = 0;
        private int orderCount = 0;
        private long lastUpdate = 0;
        private static final int MAX_WEIGHT = 1200;
        private static final int MAX_ORDERS = 100;
        private static final long COOLDOWN_MS = 60000; // 1 минута

        public boolean isLimitExceeded() {
            return usedWeight >= MAX_WEIGHT * 0.9 || orderCount >= MAX_ORDERS * 0.9;
        }

        public long getWaitTimeMs() {
            if (!isLimitExceeded()) {
                return 0;
            }

            long elapsed = System.currentTimeMillis() - lastUpdate;
            return Math.max(0, COOLDOWN_MS - elapsed);
        }

        public void setUsedWeight(int usedWeight) {
            this.usedWeight = usedWeight;
        }

        public void setOrderCount(int orderCount) {
            this.orderCount = orderCount;
        }

        public void setLastUpdate(long lastUpdate) {
            this.lastUpdate = lastUpdate;
        }
    }
}