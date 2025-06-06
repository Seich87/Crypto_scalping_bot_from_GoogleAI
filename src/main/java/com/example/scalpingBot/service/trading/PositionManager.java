package com.example.scalpingBot.service.trading;

import com.example.scalpingBot.entity.Position;
import com.example.scalpingBot.enums.OrderSide;
import com.example.scalpingBot.exception.TradingException;
import com.example.scalpingBot.repository.PositionRepository;
import com.example.scalpingBot.service.risk.PositionSizer;
import com.example.scalpingBot.service.risk.RiskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Сервис для управления жизненным циклом торговых позиций.
 * Отвечает за открытие, закрытие и мониторинг позиций.
 */
@Service
public class PositionManager {

    private static final Logger log = LoggerFactory.getLogger(PositionManager.class);

    private final PositionRepository positionRepository;
    private final OrderExecutionService orderExecutionService;
    private final PositionSizer positionSizer;
    private final RiskManager riskManager;

    @Autowired
    public PositionManager(PositionRepository positionRepository,
                           OrderExecutionService orderExecutionService,
                           PositionSizer positionSizer,
                           RiskManager riskManager) {
        this.positionRepository = positionRepository;
        this.orderExecutionService = orderExecutionService;
        this.positionSizer = positionSizer;
        this.riskManager = riskManager;
    }

    /**
     * Открывает новую позицию, если для данной пары еще нет активной.
     *
     * @param symbol      Торговая пара.
     * @param side        Сторона сделки (BUY для открытия long).
     * @param entryPrice  Цена входа в позицию.
     */
    @Transactional
    public void openPosition(String symbol, OrderSide side, BigDecimal entryPrice) {
        Optional<Position> existingPosition = positionRepository.findByTradingPairAndIsActive(symbol, true);
        if (existingPosition.isPresent()) {
            log.warn("Attempted to open a position for {}, but an active position already exists.", symbol);
            return;
        }

        // 1. Рассчитать размер позиции
        BigDecimal quantity = positionSizer.calculatePositionSize(symbol, entryPrice);
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Calculated position size for {} is zero or less. Skipping opening position.", symbol);
            return;
        }

        // 2. Исполнить ордер на покупку/продажу
        orderExecutionService.executeOrder(symbol, side, quantity);

        // 3. Рассчитать стоп-лосс и тейк-профит
        BigDecimal stopLossPrice = riskManager.calculateStopLossPrice(entryPrice, side);
        BigDecimal takeProfitPrice = riskManager.calculateTakeProfitPrice(entryPrice, side);

        // 4. Создать и сохранить запись о позиции в БД
        Position newPosition = Position.builder()
                .tradingPair(symbol)
                .side(side)
                .quantity(quantity)
                .entryPrice(entryPrice)
                .stopLossPrice(stopLossPrice)
                .takeProfitPrice(takeProfitPrice)
                .build();

        positionRepository.save(newPosition);
        log.info("Successfully opened {} position for {} of {} at price {}", side, quantity, symbol, entryPrice);
    }

    /**
     * Закрывает активную позицию для указанной торговой пары.
     *
     * @param symbol      Торговая пара.
     * @param exitPrice   Цена закрытия позиции.
     */
    @Transactional
    public void closePosition(String symbol, BigDecimal exitPrice) {
        Position activePosition = positionRepository.findByTradingPairAndIsActive(symbol, true)
                .orElseThrow(() -> new TradingException("No active position found for " + symbol + " to close."));

        // 1. Определить сторону ордера для закрытия
        // Если была покупка (long), то для закрытия нужна продажа
        OrderSide closingSide = (activePosition.getSide() == OrderSide.BUY) ? OrderSide.SELL : OrderSide.BUY;

        // 2. Исполнить ордер на закрытие
        orderExecutionService.executeOrder(symbol, closingSide, activePosition.getQuantity());

        // 3. Рассчитать P&L (Прибыль/Убыток)
        BigDecimal pnl = riskManager.calculatePnl(activePosition, exitPrice);

        // 4. Обновить запись о позиции в БД
        activePosition.setActive(false);
        activePosition.setCloseTimestamp(LocalDateTime.now());
        activePosition.setPnl(pnl);
        positionRepository.save(activePosition);

        log.info("Successfully closed position for {}. P&L: {}", symbol, pnl.toPlainString());
    }

    /**
     * Получает текущую активную позицию для торговой пары.
     * @param symbol Торговая пара.
     * @return Optional, содержащий активную позицию, если она есть.
     */
    @Transactional(readOnly = true)
    public Optional<Position> getActivePosition(String symbol) {
        return positionRepository.findByTradingPairAndIsActive(symbol, true);
    }
}