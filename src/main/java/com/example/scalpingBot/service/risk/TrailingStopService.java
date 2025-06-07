package com.example.scalpingBot.service.risk;

import com.example.scalpingBot.entity.Position;
import com.example.scalpingBot.enums.OrderSide;
import com.example.scalpingBot.repository.PositionRepository;
import com.example.scalpingBot.utils.MathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class TrailingStopService {

    private static final Logger log = LoggerFactory.getLogger(TrailingStopService.class);
    private final PositionRepository positionRepository;

    public TrailingStopService(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    /**
     * Проверяет и обновляет трейлинг-стоп для активной позиции.
     * @param position Активная позиция.
     * @param currentPrice Текущая рыночная цена.
     */
    @Transactional
    public void updateTrailingStop(Position position, BigDecimal currentPrice) {
        if (position == null || !position.isActive() || position.getTrailingStopPercentage() == null) {
            return; // Трейлинг-стоп неактивен для этой позиции
        }

        BigDecimal newStopLoss;
        BigDecimal currentStopLoss = position.getStopLossPrice();
        BigDecimal trailingPercent = position.getTrailingStopPercentage().negate(); // -2.5%

        if (position.getSide() == OrderSide.BUY) {
            // Для LONG-позиции: новый стоп-лосс = текущая цена - процент
            newStopLoss = MathUtils.applyPercentage(currentPrice, trailingPercent);
            // Мы обновляем стоп-лосс, только если он выше текущего
            if (newStopLoss.compareTo(currentStopLoss) > 0) {
                position.setStopLossPrice(newStopLoss);
                positionRepository.save(position);
                log.info("Trailing stop for {} [BUY] updated from {} to {}",
                        position.getTradingPair(), currentStopLoss, newStopLoss);
            }
        } else { // OrderSide.SELL
            // Для SHORT-позиции: новый стоп-лосс = текущая цена + процент
            newStopLoss = MathUtils.applyPercentage(currentPrice, trailingPercent.negate());
            // Мы обновляем стоп-лосс, только если он НИЖЕ текущего
            if (newStopLoss.compareTo(currentStopLoss) < 0) {
                position.setStopLossPrice(newStopLoss);
                positionRepository.save(position);
                log.info("Trailing stop for {} [SELL] updated from {} to {}",
                        position.getTradingPair(), currentStopLoss, newStopLoss);
            }
        }
    }
}