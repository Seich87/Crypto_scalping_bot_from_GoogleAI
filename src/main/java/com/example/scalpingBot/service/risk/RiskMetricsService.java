package com.example.scalpingBot.service.risk;

import com.example.scalpingBot.dto.response.RiskMetricsResponse;
import com.example.scalpingBot.entity.Position;
import com.example.scalpingBot.repository.PositionRepository;
import com.example.scalpingBot.utils.MathUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

/**
 * Сервис для расчета и предоставления сводных метрик производительности.
 * Эта реализация является полной и учитывает все необходимые детали для точного анализа.
 */
@Service
public class RiskMetricsService {

    private final PositionRepository positionRepository;
    private final BigDecimal initialCapital;

    // Используем контекст с хорошей точностью для финальных расчетов (проценты, факторы)
    private static final MathContext MC = new MathContext(6, RoundingMode.HALF_UP);

    @Autowired
    public RiskMetricsService(PositionRepository positionRepository,
                              @Value("${risk.initial-capital:10000}") BigDecimal initialCapital) {
        this.positionRepository = positionRepository;
        this.initialCapital = initialCapital;
    }

    /**
     * Рассчитывает все ключевые метрики производительности на основе
     * истории всех закрытых позиций.
     *
     * @return DTO с детально рассчитанными метриками.
     */
    @Transactional(readOnly = true)
    public RiskMetricsResponse calculateCurrentMetrics() {
        // Получаем все закрытые позиции, обязательно отсортированные по времени
        List<Position> closedPositions = positionRepository.findAllByIsActiveOrderByCloseTimestampAsc(false);

        if (closedPositions.isEmpty()) {
            return RiskMetricsResponse.builder()
                    .totalRealizedPnl(BigDecimal.ZERO)
                    .winRate(BigDecimal.ZERO)
                    .profitFactor(BigDecimal.ZERO)
                    .maxDrawdown(BigDecimal.ZERO)
                    .totalTrades(0).winningTrades(0).losingTrades(0)
                    .averageTradePnl(BigDecimal.ZERO)
                    .averageLosingTrade(BigDecimal.ZERO)
                    .averageWinningTrade(BigDecimal.ZERO)
                    .build();
        }

        BigDecimal totalPnl = BigDecimal.ZERO;
        BigDecimal grossProfit = BigDecimal.ZERO;
        BigDecimal grossLoss = BigDecimal.ZERO;
        int winningTrades = 0;

        // Для расчета просадки
        BigDecimal cumulativePnl = BigDecimal.ZERO;
        BigDecimal peakEquity = initialCapital; // Начинаем с начального капитала
        BigDecimal maxDrawdownValue = BigDecimal.ZERO; // Максимальная просадка в денежном выражении

        for (Position pos : closedPositions) {
            BigDecimal pnl = pos.getPnl();
            totalPnl = totalPnl.add(pnl);

            if (MathUtils.isPositive(pnl)) {
                winningTrades++;
                grossProfit = grossProfit.add(pnl);
            } else {
                grossLoss = grossLoss.add(pnl.abs());
            }

            // Расчет кривой капитала для просадки
            cumulativePnl = cumulativePnl.add(pnl);
            BigDecimal currentEquity = initialCapital.add(cumulativePnl);

            if (currentEquity.compareTo(peakEquity) > 0) {
                peakEquity = currentEquity; // Новый пик
            }

            BigDecimal currentDrawdown = peakEquity.subtract(currentEquity);
            if (currentDrawdown.compareTo(maxDrawdownValue) > 0) {
                maxDrawdownValue = currentDrawdown; // Новая максимальная просадка
            }
        }

        int totalTrades = closedPositions.size();
        int losingTrades = totalTrades - winningTrades;

        BigDecimal winRate = new BigDecimal(winningTrades).divide(new BigDecimal(totalTrades), MC).multiply(new BigDecimal(100));

        BigDecimal profitFactor = (grossLoss.compareTo(BigDecimal.ZERO) > 0)
                ? grossProfit.divide(grossLoss, MC)
                : new BigDecimal(Long.MAX_VALUE); // Если убытков не было, профит-фактор стремится к бесконечности

        BigDecimal maxDrawdownPercentage = (peakEquity.compareTo(BigDecimal.ZERO) > 0)
                ? maxDrawdownValue.divide(peakEquity, MC).multiply(new BigDecimal(100))
                : BigDecimal.ZERO;

        BigDecimal avgTrade = totalPnl.divide(new BigDecimal(totalTrades), MC);
        BigDecimal avgWin = (winningTrades > 0) ? grossProfit.divide(new BigDecimal(winningTrades), MC) : BigDecimal.ZERO;
        BigDecimal avgLoss = (losingTrades > 0) ? grossLoss.divide(new BigDecimal(losingTrades), MC).negate() : BigDecimal.ZERO;

        return RiskMetricsResponse.builder()
                .totalRealizedPnl(totalPnl.setScale(2, RoundingMode.HALF_UP))
                .winRate(winRate.setScale(2, RoundingMode.HALF_UP))
                .profitFactor(profitFactor.setScale(2, RoundingMode.HALF_UP))
                .maxDrawdown(maxDrawdownPercentage.setScale(2, RoundingMode.HALF_UP))
                .totalTrades(totalTrades)
                .winningTrades(winningTrades)
                .losingTrades(losingTrades)
                .averageTradePnl(avgTrade.setScale(2, RoundingMode.HALF_UP))
                .averageWinningTrade(avgWin.setScale(2, RoundingMode.HALF_UP))
                .averageLosingTrade(avgLoss.setScale(2, RoundingMode.HALF_UP))
                .build();
    }
}