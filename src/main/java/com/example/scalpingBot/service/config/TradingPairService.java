package com.example.scalpingBot.service.config;

import com.example.scalpingBot.entity.TradingPair;
import com.example.scalpingBot.enums.TradingPairType;
import com.example.scalpingBot.exception.TradingException;
import com.example.scalpingBot.repository.TradingPairRepository;
import com.example.scalpingBot.utils.MathUtils;
import com.example.scalpingBot.utils.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Сервис для управления конфигурацией торговых пар и валидации параметров ордеров.
 */
@Service
public class TradingPairService {

    private static final Logger log = LoggerFactory.getLogger(TradingPairService.class);

    private final TradingPairRepository tradingPairRepository;

    @Autowired
    public TradingPairService(TradingPairRepository tradingPairRepository) {
        this.tradingPairRepository = tradingPairRepository;
    }

    /**
     * Получает конфигурацию торговой пары с кэшированием.
     */
    @Cacheable(value = "tradingPairs", key = "#symbol")
    @Transactional(readOnly = true)
    public Optional<TradingPair> getTradingPair(String symbol) {
        ValidationUtils.assertNotBlank(symbol, "Trading pair symbol cannot be blank");
        return tradingPairRepository.findBySymbol(symbol.toUpperCase());
    }

    /**
     * Получает и валидирует активную торговую пару.
     */
    @Transactional(readOnly = true)
    public TradingPair getActiveTradingPair(String symbol) {
        Optional<TradingPair> tradingPairOpt = getTradingPair(symbol);

        if (tradingPairOpt.isEmpty()) {
            throw new TradingException("Trading pair not found: " + symbol);
        }

        TradingPair tradingPair = tradingPairOpt.get();

        if (!tradingPair.isActive()) {
            throw new TradingException("Trading pair is not active: " + symbol);
        }

        return tradingPair;
    }

    /**
     * Получает все активные торговые пары.
     */
    @Cacheable(value = "activeTradingPairs")
    @Transactional(readOnly = true)
    public List<TradingPair> getActiveTradingPairs() {
        return tradingPairRepository.findAllByIsActive(true);
    }

    /**
     * Получает активные торговые пары определенного типа.
     */
    @Transactional(readOnly = true)
    public List<TradingPair> getActiveTradingPairs(TradingPairType type) {
        ValidationUtils.assertNotNull(type, "Trading pair type cannot be null");
        return tradingPairRepository.findAllByIsActiveAndType(true, type);
    }

    /**
     * Валидирует и округляет количество согласно точности торговой пары.
     */
    public BigDecimal validateAndAdjustQuantity(String symbol, BigDecimal quantity) {
        ValidationUtils.assertPositive(quantity, "Quantity must be positive");

        TradingPair tradingPair = getActiveTradingPair(symbol);

        // Округляем количество согласно точности
        BigDecimal adjustedQuantity = MathUtils.scale(quantity, tradingPair.getQuantityPrecision());

        log.debug("Adjusted quantity for {}: {} -> {} (precision: {})",
                symbol, quantity.toPlainString(), adjustedQuantity.toPlainString(),
                tradingPair.getQuantityPrecision());

        return adjustedQuantity;
    }

    /**
     * Валидирует и округляет цену согласно точности торговой пары.
     */
    public BigDecimal validateAndAdjustPrice(String symbol, BigDecimal price) {
        ValidationUtils.assertPositive(price, "Price must be positive");

        TradingPair tradingPair = getActiveTradingPair(symbol);

        // Округляем цену согласно точности
        BigDecimal adjustedPrice = MathUtils.scale(price, tradingPair.getPricePrecision());

        log.debug("Adjusted price for {}: {} -> {} (precision: {})",
                symbol, price.toPlainString(), adjustedPrice.toPlainString(),
                tradingPair.getPricePrecision());

        return adjustedPrice;
    }

    /**
     * Валидирует размер ордера согласно минимальным требованиям.
     */
    public void validateOrderSize(String symbol, BigDecimal quantity, BigDecimal price) {
        TradingPair tradingPair = getActiveTradingPair(symbol);

        if (tradingPair.getMinOrderSize() == null) {
            return; // Нет ограничений на минимальный размер
        }

        // Рассчитываем общую стоимость ордера в квотируемой валюте
        BigDecimal orderValue = quantity.multiply(price);

        if (orderValue.compareTo(tradingPair.getMinOrderSize()) < 0) {
            throw new TradingException(String.format(
                    "Order size too small for %s. Required: %s %s, provided: %s %s",
                    symbol,
                    tradingPair.getMinOrderSize().toPlainString(),
                    tradingPair.getQuoteAsset(),
                    orderValue.toPlainString(),
                    tradingPair.getQuoteAsset()
            ));
        }

        log.debug("Order size validation passed for {}: {} {} (min: {} {})",
                symbol, orderValue.toPlainString(), tradingPair.getQuoteAsset(),
                tradingPair.getMinOrderSize().toPlainString(), tradingPair.getQuoteAsset());
    }

    /**
     * Проверяет, доступна ли торговая пара для торговли.
     */
    public boolean isTradingAllowed(String symbol) {
        try {
            TradingPair tradingPair = getActiveTradingPair(symbol);
            return tradingPair.isActive();
        } catch (TradingException e) {
            return false;
        }
    }

    /**
     * Получает базовый актив из символа торговой пары.
     */
    public String getBaseAsset(String symbol) {
        TradingPair tradingPair = getActiveTradingPair(symbol);
        return tradingPair.getBaseAsset();
    }

    /**
     * Получает квотируемый актив из символа торговой пары.
     */
    public String getQuoteAsset(String symbol) {
        TradingPair tradingPair = getActiveTradingPair(symbol);
        return tradingPair.getQuoteAsset();
    }

    /**
     * Создает или обновляет конфигурацию торговой пары.
     */
    @Transactional
    public TradingPair createOrUpdateTradingPair(String symbol, String baseAsset, String quoteAsset,
                                                 TradingPairType type, boolean isActive,
                                                 int pricePrecision, int quantityPrecision,
                                                 BigDecimal minOrderSize) {

        ValidationUtils.assertNotBlank(symbol, "Symbol cannot be blank");
        ValidationUtils.assertNotBlank(baseAsset, "Base asset cannot be blank");
        ValidationUtils.assertNotBlank(quoteAsset, "Quote asset cannot be blank");
        ValidationUtils.assertNotNull(type, "Trading pair type cannot be null");

        Optional<TradingPair> existingPair = tradingPairRepository.findBySymbol(symbol);

        TradingPair tradingPair;
        if (existingPair.isPresent()) {
            tradingPair = existingPair.get();
            log.info("Updating existing trading pair: {}", symbol);
        } else {
            tradingPair = new TradingPair();
            tradingPair.setSymbol(symbol);
            log.info("Creating new trading pair: {}", symbol);
        }

        tradingPair.setBaseAsset(baseAsset);
        tradingPair.setQuoteAsset(quoteAsset);
        tradingPair.setType(type);
        tradingPair.setActive(isActive);
        tradingPair.setPricePrecision(pricePrecision);
        tradingPair.setQuantityPrecision(quantityPrecision);
        tradingPair.setMinOrderSize(minOrderSize);

        return tradingPairRepository.save(tradingPair);
    }

    /**
     * Активирует или деактивирует торговую пару.
     */
    @Transactional
    public void setTradingPairActive(String symbol, boolean isActive) {
        TradingPair tradingPair = tradingPairRepository.findBySymbol(symbol)
                .orElseThrow(() -> new TradingException("Trading pair not found: " + symbol));

        tradingPair.setActive(isActive);
        tradingPairRepository.save(tradingPair);

        log.info("Trading pair {} is now {}", symbol, isActive ? "ACTIVE" : "INACTIVE");
    }

    /**
     * Получает торговые пары, подходящие для указанного бюджета.
     */
    @Transactional(readOnly = true)
    public List<TradingPair> getTradingPairsForBudget(BigDecimal maxOrderSize) {
        ValidationUtils.assertPositive(maxOrderSize, "Max order size must be positive");
        return tradingPairRepository.findActiveWithMinOrderSizeLessThan(maxOrderSize);
    }

    /**
     * Получает информацию о точности для отображения в UI.
     */
    public String formatQuantity(String symbol, BigDecimal quantity) {
        TradingPair tradingPair = getActiveTradingPair(symbol);
        return MathUtils.scale(quantity, tradingPair.getQuantityPrecision()).toPlainString();
    }

    /**
     * Получает информацию о точности для отображения в UI.
     */
    public String formatPrice(String symbol, BigDecimal price) {
        TradingPair tradingPair = getActiveTradingPair(symbol);
        return MathUtils.scale(price, tradingPair.getPricePrecision()).toPlainString();
    }

    /**
     * Валидирует торговую пару для скальпинга (дополнительные проверки).
     */
    public void validateForScalping(String symbol) {
        TradingPair tradingPair = getActiveTradingPair(symbol);

        // Проверки специфичные для скальпинга
        if (tradingPair.getType() != TradingPairType.SPOT) {
            log.warn("Non-SPOT trading detected for {}: {}", symbol, tradingPair.getType());
        }

        // Для скальпинга нужна высокая точность
        if (tradingPair.getPricePrecision() < 2) {
            log.warn("Low price precision for scalping {}: {}", symbol, tradingPair.getPricePrecision());
        }

        if (tradingPair.getQuantityPrecision() < 3) {
            log.warn("Low quantity precision for scalping {}: {}", symbol, tradingPair.getQuantityPrecision());
        }

        log.debug("Scalping validation passed for {}", symbol);
    }
}