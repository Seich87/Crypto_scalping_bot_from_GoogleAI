package com.example.scalpingBot.service.risk;

import com.example.scalpingBot.dto.exchange.BalanceDto;
import com.example.scalpingBot.exception.TradingException;
import com.example.scalpingBot.service.exchange.ExchangeApiService;
import com.example.scalpingBot.utils.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Сервис для расчета размера торговой позиции.
 * Определяет, какое количество актива нужно купить/продать на основе доступного баланса и правил риска.
 */
@Service
public class PositionSizer {

    private static final Logger log = LoggerFactory.getLogger(PositionSizer.class);

    private final ExchangeApiService exchangeApiService;

    // Процент от баланса, который используется для одной сделки.
    private final BigDecimal riskPerTradePercentage;
    // Квотируемая валюта, в которой измеряется баланс (например, USDT, BUSD).
    private final String quoteAsset;

    @Autowired
    public PositionSizer(@Qualifier("binance") ExchangeApiService exchangeApiService,
                         @Value("${risk.per-trade.percentage:10}") BigDecimal riskPerTradePercentage,
                         @Value("${risk.quote-asset:USDT}") String quoteAsset) {
        this.exchangeApiService = exchangeApiService;
        this.riskPerTradePercentage = riskPerTradePercentage;
        this.quoteAsset = quoteAsset;
        log.info("PositionSizer initialized with Risk-Per-Trade: {}% of {} balance.",
                riskPerTradePercentage.toPlainString(), quoteAsset);
    }

    /**
     * Рассчитывает размер позиции в базовом активе (например, в BTC).
     *
     * @param symbol     Торговая пара, например "BTCUSDT".
     * @param entryPrice Текущая цена, по которой планируется вход.
     * @return Количество базового актива для покупки.
     * @throws TradingException если не удалось рассчитать размер (например, недостаточно средств).
     */
    public BigDecimal calculatePositionSize(String symbol, BigDecimal entryPrice) {
        ValidationUtils.assertPositive(entryPrice, "Entry price must be positive for position sizing.");

        // 1. Получаем доступный баланс в квотируемой валюте (например, USDT)
        BigDecimal availableBalance = getAvailableQuoteBalance();
        log.debug("Available {} balance: {}", quoteAsset, availableBalance);

        // 2. Рассчитываем сумму, которую мы готовы рискнуть в этой сделке
        BigDecimal amountToInvest = availableBalance.multiply(riskPerTradePercentage.divide(new BigDecimal(100)));
        log.debug("Amount to invest based on {}% risk: {}", riskPerTradePercentage, amountToInvest.toPlainString());

        if (amountToInvest.compareTo(BigDecimal.ONE) < 0) { // Проверка на минимальный размер инвестиции
            log.warn("Amount to invest ({}) is too small. Not opening position.", amountToInvest.toPlainString());
            return BigDecimal.ZERO;
        }

        // 3. Рассчитываем, сколько базового актива (например, BTC) мы можем купить на эту сумму
        // Для простоты, мы не указываем точность округления здесь, в реальной системе
        // ее нужно брать из настроек торговой пары (TradingPair.quantityPrecision).
        BigDecimal quantity = amountToInvest.divide(entryPrice, 8, RoundingMode.DOWN);
        log.info("Calculated position size for {}: {} units", symbol, quantity.toPlainString());

        return quantity;
    }

    /**
     * Получает свободный баланс квотируемой валюты с биржи.
     *
     * @return Свободный баланс.
     */
    private BigDecimal getAvailableQuoteBalance() {
        return exchangeApiService.getBalances().stream()
                .filter(balance -> balance.getAsset().equalsIgnoreCase(quoteAsset))
                .findFirst()
                .map(BalanceDto::getFree)
                .orElseThrow(() -> new TradingException("Could not find balance for quote asset: " + quoteAsset));
    }
}