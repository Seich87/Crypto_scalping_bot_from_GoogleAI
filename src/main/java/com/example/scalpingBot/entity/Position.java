package com.example.scalpingBot.entity;

import com.example.scalpingBot.enums.OrderSide;
import com.example.scalpingBot.enums.RiskLevel;
import com.example.scalpingBot.enums.TradingPairType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Сущность торговой позиции для скальпинг-бота
 *
 * Представляет агрегированную информацию о позиции по торговой паре:
 * - Текущее состояние позиции (размер, направление, P&L)
 * - Уровни риск-менеджмента (стоп-лосс, тейк-профит)
 * - История связанных торговых операций
 * - Метрики производительности и временные ограничения
 * - Интеграция с системой управления рисками
 *
 * Используется для:
 * - Управления открытыми позициями
 * - Контроля риск-лимитов
 * - Автоматического закрытия по времени (1 час макс)
 * - Расчета корреляций между позициями
 * - Мониторинга производительности стратегии
 *
 * @author ScalpingBot Team
 * @version 1.0
 */
@Entity
@Table(name = "positions", indexes = {
        @Index(name = "idx_position_pair", columnList = "tradingPair"),
        @Index(name = "idx_position_status", columnList = "status"),
        @Index(name = "idx_position_opened", columnList = "openedAt"),
        @Index(name = "idx_position_pnl", columnList = "unrealizedPnl"),
        @Index(name = "idx_position_risk", columnList = "riskLevel"),
        @Index(name = "idx_position_active", columnList = "isActive")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Position {

    /**
     * Уникальный идентификатор позиции
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Торговая пара
     */
    @Column(name = "trading_pair", nullable = false, length = 20)
    private String tradingPair;

    /**
     * Тип торговой пары
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "pair_type", nullable = false, length = 20)
    private TradingPairType pairType;

    /**
     * Направление позиции (LONG/SHORT)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false, length = 10)
    private OrderSide side;

    /**
     * Статус позиции
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PositionStatus status;

    /**
     * Активна ли позиция
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    /**
     * Размер позиции (количество базового актива)
     */
    @Column(name = "size", nullable = false, precision = 18, scale = 8)
    private BigDecimal size;

    /**
     * Средняя цена входа
     */
    @Column(name = "entry_price", nullable = false, precision = 18, scale = 8)
    private BigDecimal entryPrice;

    /**
     * Текущая рыночная цена
     */
    @Column(name = "current_price", precision = 18, scale = 8)
    private BigDecimal currentPrice;

    /**
     * Цена закрытия позиции
     */
    @Column(name = "exit_price", precision = 18, scale = 8)
    private BigDecimal exitPrice;

    /**
     * Стоимость позиции в USDT на момент входа
     */
    @Column(name = "entry_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal entryValue;

    /**
     * Текущая стоимость позиции в USDT
     */
    @Column(name = "current_value", precision = 18, scale = 2)
    private BigDecimal currentValue;

    /**
     * Нереализованная прибыль/убыток в USDT
     */
    @Column(name = "unrealized_pnl", precision = 18, scale = 2)
    private BigDecimal unrealizedPnl;

    /**
     * Нереализованная прибыль/убыток в процентах
     */
    @Column(name = "unrealized_pnl_percent", precision = 8, scale = 4)
    private BigDecimal unrealizedPnlPercent;

    /**
     * Реализованная прибыль/убыток в USDT
     */
    @Column(name = "realized_pnl", precision = 18, scale = 2)
    private BigDecimal realizedPnl;

    /**
     * Реализованная прибыль/убыток в процентах
     */
    @Column(name = "realized_pnl_percent", precision = 8, scale = 4)
    private BigDecimal realizedPnlPercent;

    /**
     * Общие комиссии по позиции
     */
    @Column(name = "total_commission", precision = 18, scale = 8)
    private BigDecimal totalCommission;

    /**
     * Цена стоп-лосса
     */
    @Column(name = "stop_loss_price", precision = 18, scale = 8)
    private BigDecimal stopLossPrice;

    /**
     * ID ордера стоп-лосса на бирже
     */
    @Column(name = "stop_loss_order_id", length = 50)
    private String stopLossOrderId;

    /**
     * Цена тейк-профита
     */
    @Column(name = "take_profit_price", precision = 18, scale = 8)
    private BigDecimal takeProfitPrice;

    /**
     * ID ордера тейк-профита на бирже
     */
    @Column(name = "take_profit_order_id", length = 50)
    private String takeProfitOrderId;

    /**
     * Trailing stop включен
     */
    @Column(name = "trailing_stop_enabled")
    private Boolean trailingStopEnabled;

    /**
     * Размер trailing stop в процентах
     */
    @Column(name = "trailing_stop_percent", precision = 8, scale = 4)
    private BigDecimal trailingStopPercent;

    /**
     * Максимальная цена для trailing stop
     */
    @Column(name = "trailing_stop_max_price", precision = 18, scale = 8)
    private BigDecimal trailingStopMaxPrice;

    /**
     * Текущий уровень риска позиции
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 20)
    private RiskLevel riskLevel;

    /**
     * Процент от общего капитала
     */
    @Column(name = "portfolio_percent", precision = 8, scale = 4)
    private BigDecimal portfolioPercent;

    /**
     * Название стратегии
     */
    @Column(name = "strategy_name", length = 50)
    private String strategyName;

    /**
     * Название биржи
     */
    @Column(name = "exchange_name", nullable = false, length = 20)
    private String exchangeName;

    /**
     * Время открытия позиции
     */
    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    /**
     * Время закрытия позиции
     */
    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    /**
     * Время последнего обновления
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Максимальное время удержания в минутах
     */
    @Column(name = "max_holding_time_minutes", nullable = false)
    private Integer maxHoldingTimeMinutes;

    /**
     * Время до принудительного закрытия
     */
    @Column(name = "force_close_at")
    private LocalDateTime forceCloseAt;

    /**
     * Максимальная прибыль с момента открытия
     */
    @Column(name = "max_profit", precision = 18, scale = 2)
    private BigDecimal maxProfit;

    /**
     * Максимальная просадка с момента открытия
     */
    @Column(name = "max_drawdown", precision = 18, scale = 2)
    private BigDecimal maxDrawdown;

    /**
     * Максимальная прибыль в процентах
     */
    @Column(name = "max_profit_percent", precision = 8, scale = 4)
    private BigDecimal maxProfitPercent;

    /**
     * Максимальная просадка в процентах
     */
    @Column(name = "max_drawdown_percent", precision = 8, scale = 4)
    private BigDecimal maxDrawdownPercent;

    /**
     * Причина закрытия позиции
     */
    @Column(name = "close_reason", length = 50)
    private String closeReason;

    /**
     * Количество связанных сделок
     */
    @Column(name = "trades_count")
    private Integer tradesCount;

    /**
     * Средний спред при входе
     */
    @Column(name = "avg_spread", precision = 8, scale = 4)
    private BigDecimal avgSpread;

    /**
     * Волатильность на момент входа (ATR)
     */
    @Column(name = "entry_volatility", precision = 8, scale = 4)
    private BigDecimal entryVolatility;

    /**
     * Сила сигнала на момент входа
     */
    @Column(name = "entry_signal_strength", precision = 8, scale = 4)
    private BigDecimal entrySignalStrength;

    /**
     * Дополнительные метаданные
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * Комментарии к позиции
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Версия для оптимистичной блокировки
     */
    @Version
    private Long version;

    /**
     * Связанные торговые операции
     */
    @OneToMany(mappedBy = "positionId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Trade> trades = new ArrayList<>();

    /**
     * Перечисление статусов позиции
     */
    public enum PositionStatus {
        OPENING,      // Позиция открывается
        OPEN,         // Позиция открыта
        REDUCING,     // Позиция сокращается
        CLOSING,      // Позиция закрывается
        CLOSED,       // Позиция закрыта
        FAILED        // Ошибка открытия/закрытия
    }

    /**
     * Автоматическое обновление временных меток
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Автоматическая инициализация при создании
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.openedAt = now;
        this.updatedAt = now;

        // Значения по умолчанию
        if (this.isActive == null) {
            this.isActive = true;
        }
        if (this.status == null) {
            this.status = PositionStatus.OPENING;
        }
        if (this.maxHoldingTimeMinutes == null) {
            this.maxHoldingTimeMinutes = 60; // 1 час по умолчанию для скальпинга
        }
        if (this.trailingStopEnabled == null) {
            this.trailingStopEnabled = false;
        }
        if (this.tradesCount == null) {
            this.tradesCount = 0;
        }

        // Рассчитываем время принудительного закрытия
        this.forceCloseAt = now.plusMinutes(this.maxHoldingTimeMinutes);

        // Инициализируем P&L
        if (this.unrealizedPnl == null) {
            this.unrealizedPnl = BigDecimal.ZERO;
        }
        if (this.realizedPnl == null) {
            this.realizedPnl = BigDecimal.ZERO;
        }
    }

    /**
     * Проверить, является ли позиция длинной
     */
    public boolean isLong() {
        return side == OrderSide.BUY;
    }

    /**
     * Проверить, является ли позиция короткой
     */
    public boolean isShort() {
        return side == OrderSide.SELL;
    }

    /**
     * Проверить, является ли позиция прибыльной
     */
    public boolean isProfitable() {
        BigDecimal totalPnl = getTotalPnl();
        return totalPnl != null && totalPnl.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Проверить, является ли позиция убыточной
     */
    public boolean isLoss() {
        BigDecimal totalPnl = getTotalPnl();
        return totalPnl != null && totalPnl.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Получить общий P&L (реализованный + нереализованный)
     */
    public BigDecimal getTotalPnl() {
        BigDecimal realized = realizedPnl != null ? realizedPnl : BigDecimal.ZERO;
        BigDecimal unrealized = unrealizedPnl != null ? unrealizedPnl : BigDecimal.ZERO;
        return realized.add(unrealized);
    }

    /**
     * Получить общий P&L в процентах
     */
    public BigDecimal getTotalPnlPercent() {
        if (entryValue == null || entryValue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalPnl = getTotalPnl();
        return totalPnl.divide(entryValue, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    /**
     * Проверить, истекло ли время удержания позиции
     */
    public boolean isExpired() {
        return forceCloseAt != null && LocalDateTime.now().isAfter(forceCloseAt);
    }

    /**
     * Получить оставшееся время удержания в минутах
     */
    public long getRemainingTimeMinutes() {
        if (forceCloseAt == null) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(forceCloseAt)) {
            return 0;
        }

        return java.time.Duration.between(now, forceCloseAt).toMinutes();
    }

    /**
     * Получить время удержания позиции в минутах
     */
    public long getHoldingTimeMinutes() {
        LocalDateTime endTime = closedAt != null ? closedAt : LocalDateTime.now();
        return java.time.Duration.between(openedAt, endTime).toMinutes();
    }

    /**
     * Проверить, достигнут ли стоп-лосс
     */
    public boolean isStopLossTriggered() {
        if (stopLossPrice == null || currentPrice == null) {
            return false;
        }

        if (isLong()) {
            return currentPrice.compareTo(stopLossPrice) <= 0;
        } else {
            return currentPrice.compareTo(stopLossPrice) >= 0;
        }
    }

    /**
     * Проверить, достигнут ли тейк-профит
     */
    public boolean isTakeProfitTriggered() {
        if (takeProfitPrice == null || currentPrice == null) {
            return false;
        }

        if (isLong()) {
            return currentPrice.compareTo(takeProfitPrice) >= 0;
        } else {
            return currentPrice.compareTo(takeProfitPrice) <= 0;
        }
    }

    /**
     * Обновить текущую цену и пересчитать P&L
     */
    public void updateCurrentPrice(BigDecimal newPrice) {
        this.currentPrice = newPrice;
        updateUnrealizedPnl();
        updateMaxProfitDrawdown();
        updateTrailingStop();
    }

    /**
     * Пересчитать нереализованный P&L
     */
    private void updateUnrealizedPnl() {
        if (currentPrice == null || entryPrice == null || size == null) {
            return;
        }

        BigDecimal priceDiff = currentPrice.subtract(entryPrice);
        if (isShort()) {
            priceDiff = priceDiff.negate();
        }

        this.unrealizedPnl = size.multiply(priceDiff);
        this.currentValue = size.multiply(currentPrice);

        if (entryValue != null && entryValue.compareTo(BigDecimal.ZERO) > 0) {
            this.unrealizedPnlPercent = unrealizedPnl.divide(entryValue, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
    }

    /**
     * Обновить максимальную прибыль и просадку
     */
    private void updateMaxProfitDrawdown() {
        if (unrealizedPnl == null) {
            return;
        }

        // Обновляем максимальную прибыль
        if (maxProfit == null || unrealizedPnl.compareTo(maxProfit) > 0) {
            this.maxProfit = unrealizedPnl;
            this.maxProfitPercent = unrealizedPnlPercent;
        }

        // Обновляем максимальную просадку
        if (maxDrawdown == null || unrealizedPnl.compareTo(maxDrawdown) < 0) {
            this.maxDrawdown = unrealizedPnl;
            this.maxDrawdownPercent = unrealizedPnlPercent;
        }
    }

    /**
     * Обновить trailing stop
     */
    private void updateTrailingStop() {
        if (!trailingStopEnabled || trailingStopPercent == null || currentPrice == null) {
            return;
        }

        if (isLong()) {
            // Для длинной позиции обновляем максимальную цену
            if (trailingStopMaxPrice == null || currentPrice.compareTo(trailingStopMaxPrice) > 0) {
                this.trailingStopMaxPrice = currentPrice;

                // Пересчитываем стоп-лосс
                BigDecimal stopDistance = currentPrice.multiply(trailingStopPercent)
                        .divide(new BigDecimal("100"));
                this.stopLossPrice = currentPrice.subtract(stopDistance);
            }
        } else {
            // Для короткой позиции обновляем минимальную цену
            if (trailingStopMaxPrice == null || currentPrice.compareTo(trailingStopMaxPrice) < 0) {
                this.trailingStopMaxPrice = currentPrice;

                // Пересчитываем стоп-лосс
                BigDecimal stopDistance = currentPrice.multiply(trailingStopPercent)
                        .divide(new BigDecimal("100"));
                this.stopLossPrice = currentPrice.add(stopDistance);
            }
        }
    }

    /**
     * Добавить связанную торговую операцию
     */
    public void addTrade(Trade trade) {
        if (trades == null) {
            trades = new ArrayList<>();
        }
        trades.add(trade);
        trade.setPositionId(this.id);
        this.tradesCount = trades.size();
    }

    /**
     * Закрыть позицию
     */
    public void closePosition(BigDecimal exitPrice, String reason) {
        this.exitPrice = exitPrice;
        this.currentPrice = exitPrice;
        this.closeReason = reason;
        this.status = PositionStatus.CLOSED;
        this.isActive = false;
        this.closedAt = LocalDateTime.now();

        // Перемещаем нереализованный P&L в реализованный
        if (this.unrealizedPnl != null) {
            this.realizedPnl = (this.realizedPnl != null ? this.realizedPnl : BigDecimal.ZERO)
                    .add(this.unrealizedPnl);
            this.realizedPnlPercent = this.unrealizedPnlPercent;
            this.unrealizedPnl = BigDecimal.ZERO;
            this.unrealizedPnlPercent = BigDecimal.ZERO;
        }
    }

    @Override
    public String toString() {
        return String.format("Position{id=%d, pair='%s', %s, size=%.8f, entry=%.8f, current=%.8f, pnl=%.2f (%.2f%%), status=%s}",
                id, tradingPair, side,
                size != null ? size : BigDecimal.ZERO,
                entryPrice != null ? entryPrice : BigDecimal.ZERO,
                currentPrice != null ? currentPrice : BigDecimal.ZERO,
                getTotalPnl(),
                getTotalPnlPercent(),
                status);
    }
}