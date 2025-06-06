package com.example.scalpingBot.service.risk;

import com.example.scalpingBot.entity.Position;
import com.example.scalpingBot.enums.OrderSide;
import com.example.scalpingBot.utils.MathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Сервис для управления общими параметрами риска.
 * Отвечает за расчет PnL, а также за вычисление уровней стоп-лосса и тейк-профита.
 */
@Service
public class RiskManager {

    private static final Logger log = LoggerFactory.getLogger(RiskManager.class);

    // Параметры риска, загружаемые из application.properties
    // Эти значения могут быть переопределены для каждой торговой пары отдельно.
    private final BigDecimal defaultStopLossPercentage;
    private final BigDecimal defaultTakeProfitPercentage;

    public RiskManager(@Value("${risk.default.stop-loss.percentage:1.5}") BigDecimal defaultStopLossPercentage,
                       @Value("${risk.default.take-profit.percentage:3.0}") BigDecimal defaultTakeProfitPercentage) {
        this.defaultStopLossPercentage = defaultStopLossPercentage;
        this.defaultTakeProfitPercentage = defaultTakeProfitPercentage;
        log.info("RiskManager initialized with default StopLoss: {}%, TakeProfit: {}%",
                defaultStopLossPercentage.toPlainString(), defaultTakeProfitPercentage.toPlainString());
    }

    /**
     * Рассчитывает цену стоп-лосса на основе цены входа и процента.
     *
     * @param entryPrice Цена входа в позицию.
     * @param side       Сторона позиции (BUY или SELL).
     * @return Расчетная цена стоп-лосса.
     */
    public BigDecimal calculateStopLossPrice(BigDecimal entryPrice, OrderSide side) {
        BigDecimal percentage = defaultStopLossPercentage.negate(); // Для стоп-лосса процент отрицательный
        if (side == OrderSide.SELL) { // Для short-позиции стоп-лосс будет выше цены входа
            percentage = percentage.negate();
        }
        return MathUtils.applyPercentage(entryPrice, percentage);
    }

    /**
     * Рассчитывает цену тейк-профита на основе цены входа и процента.
     *
     * @param entryPrice Цена входа в позицию.
     * @param side       Сторона позиции (BUY или SELL).
     * @return Расчетная цена тейк-профита.
     */
    public BigDecimal calculateTakeProfitPrice(BigDecimal entryPrice, OrderSide side) {
        BigDecimal percentage = defaultTakeProfitPercentage;
        if (side == OrderSide.SELL) { // Для short-позиции тейк-профит будет ниже цены входа
            percentage = percentage.negate();
        }
        return MathUtils.applyPercentage(entryPrice, percentage);
    }

    /**
     * Рассчитывает реализованную прибыль/убыток (PnL) для закрытой позиции.
     *
     * @param position  Позиция, которая была закрыта.
     * @param exitPrice Цена закрытия позиции.
     * @return Сумма PnL в квотируемой валюте.
     */
    public BigDecimal calculatePnl(Position position, BigDecimal exitPrice) {
        BigDecimal priceDifference = exitPrice.subtract(position.getEntryPrice());

        // Для short-позиции PnL рассчитывается наоборот
        if (position.getSide() == OrderSide.SELL) {
            priceDifference = priceDifference.negate();
        }

        return priceDifference.multiply(position.getQuantity());
    }

    /**
     * Проверяет, не превышает ли потенциальный убыток максимально допустимый.
     * (Пример дополнительной проверки риска)
     *
     * @param entryPrice Цена входа.
     * @param stopLossPrice Цена стоп-лосса.
     * @param quantity Размер позиции.
     * @param maxLossInQuote Максимально допустимый убыток в квотируемой валюте (например, в USDT).
     * @return true, если риск приемлем, иначе false.
     */
    public boolean isRiskAcceptable(BigDecimal entryPrice, BigDecimal stopLossPrice, BigDecimal quantity, BigDecimal maxLossInQuote) {
        BigDecimal potentialLoss = entryPrice.subtract(stopLossPrice).abs().multiply(quantity);
        return potentialLoss.compareTo(maxLossInQuote) <= 0;
    }
}