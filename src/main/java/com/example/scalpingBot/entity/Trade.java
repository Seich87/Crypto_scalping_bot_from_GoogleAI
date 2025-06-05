package com.example.scalpingBot.entity;

import com.example.scalpingBot.enums.OrderSide;
import com.example.scalpingBot.enums.OrderStatus;
import com.example.scalpingBot.enums.OrderType;
import com.example.scalpingBot.enums.TradingPairType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Сущность торговой операции для скальпинг-бота
 *
 * Хранит полную информацию о каждой торговой операции:
 * - Параметры ордера (пара, тип, сторона, количество, цена)
 * - Статус исполнения и временные метки
 * - Финансовые результаты (P&L, комиссии)
 * - Связь с позицией и стратегией
 * - Данные биржи и технического анализа
 *
 * Используется для:
 * - Учета всех торговых операций
 * - Расчета статистики и производительности
 * - Анализа стратегии и оптимизации
 * - Аудита и соблюдения требований
 *
 * @author ScalpingBot Team
 * @version 1.0
 */
@Entity
@Table(name = "trades", indexes = {
        @Index(name = "idx_trade_timestamp", columnList = "createdAt"),
        @Index(name = "idx_trade_pair", columnList = "tradingPair"),
        @Index(name = "idx_trade_status", columnList = "status"),
        @Index(name = "idx_trade_position", columnList = "positionId"),
        @Index(name = "idx_trade_exchange_id", columnList = "exchangeOrderId"),
        @Index(name = "idx_trade_pnl", columnList = "realizedPnl")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trade {

    /**
     * Уникальный идентификатор торговой операции
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Торговая пара (BTCUSDT, ETHUSDT, etc.)
     */
    @Column(name = "trading_pair", nullable = false, length = 20)
    private String tradingPair;

    /**
     * Тип торговой пары (для категоризации)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "pair_type", nullable = false, length = 20)
    private TradingPairType pairType;

    /**
     * Тип ордера
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 20)
    private OrderType orderType;

    /**
     * Сторона ордера (покупка/продажа)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "order_side", nullable = false, length = 10)
    private OrderSide orderSide;

    /**
     * Статус ордера
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    /**
     * Количество актива
     */
    @Column(name = "quantity", nullable = false, precision = 18, scale = 8)
    private BigDecimal quantity;

    /**
     * Исполненное количество
     */
    @Column(name = "executed_quantity", precision = 18, scale = 8)
    private BigDecimal executedQuantity;

    /**
     * Цена ордера
     */
    @Column(name = "price", precision = 18, scale = 8)
    private BigDecimal price;

    /**
     * Средняя цена исполнения
     */
    @Column(name = "avg_price", precision = 18, scale = 8)
    private BigDecimal avgPrice;

    /**
     * Общая стоимость в котируемой валюте (USDT)
     */
    @Column(name = "total_value", precision = 18, scale = 2)
    private BigDecimal totalValue;

    /**
     * Комиссия за сделку
     */
    @Column(name = "commission", precision = 18, scale = 8)
    private BigDecimal commission;

    /**
     * Актив комиссии (обычно BNB или USDT)
     */
    @Column(name = "commission_asset", length = 10)
    private String commissionAsset;

    /**
     * Реализованная прибыль/убыток
     */
    @Column(name = "realized_pnl", precision = 18, scale = 2)
    private BigDecimal realizedPnl;

    /**
     * Реализованная прибыль/убыток в процентах
     */
    @Column(name = "realized_pnl_percent", precision = 8, scale = 4)
    private BigDecimal realizedPnlPercent;

    /**
     * ID позиции (связь с Position entity)
     */
    @Column(name = "position_id")
    private Long positionId;

    /**
     * ID ордера на бирже
     */
    @Column(name = "exchange_order_id", length = 50)
    private String exchangeOrderId;

    /**
     * Название биржи
     */
    @Column(name = "exchange_name", nullable = false, length = 20)
    private String exchangeName;

    /**
     * ID клиентского ордера
     */
    @Column(name = "client_order_id", length = 50)
    private String clientOrderId;

    /**
     * Время создания ордера
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Время обновления ордера
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Время исполнения ордера
     */
    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    /**
     * Время отмены ордера
     */
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    /**
     * Временная метка биржи
     */
    @Column(name = "exchange_timestamp")
    private Long exchangeTimestamp;

    /**
     * Является ли ордер частью скальпинг стратегии
     */
    @Column(name = "is_scalping", nullable = false)
    private Boolean isScalping;

    /**
     * Название стратегии
     */
    @Column(name = "strategy_name", length = 50)
    private String strategyName;

    /**
     * Цена входа в позицию (для расчета P&L)
     */
    @Column(name = "entry_price", precision = 18, scale = 8)
    private BigDecimal entryPrice;

    /**
     * Цена стоп-лосса
     */
    @Column(name = "stop_loss_price", precision = 18, scale = 8)
    private BigDecimal stopLossPrice;

    /**
     * Цена тейк-профита
     */
    @Column(name = "take_profit_price", precision = 18, scale = 8)
    private BigDecimal takeProfitPrice;

    /**
     * Причина закрытия позиции
     */
    @Column(name = "close_reason", length = 50)
    private String closeReason;

    /**
     * Время удержания позиции в минутах
     */
    @Column(name = "holding_time_minutes")
    private Integer holdingTimeMinutes;

    /**
     * Спред на момент исполнения в процентах
     */
    @Column(name = "spread_percent", precision = 8, scale = 4)
    private BigDecimal spreadPercent;

    /**
     * Проскальзывание в процентах
     */
    @Column(name = "slippage_percent", precision = 8, scale = 4)
    private BigDecimal slippagePercent;

    /**
     * Объем торгов в момент сделки
     */
    @Column(name = "market_volume_24h", precision = 18, scale = 2)
    private BigDecimal marketVolume24h;

    /**
     * Цена на момент принятия решения
     */
    @Column(name = "decision_price", precision = 18, scale = 8)
    private BigDecimal decisionPrice;

    /**
     * RSI на момент принятия решения
     */
    @Column(name = "rsi_value", precision = 8, scale = 4)
    private BigDecimal rsiValue;

    /**
     * EMA значение
     */
    @Column(name = "ema_value", precision = 18, scale = 8)
    private BigDecimal emaValue;

    /**
     * MACD значение
     */
    @Column(name = "macd_value", precision = 18, scale = 8)
    private BigDecimal macdValue;

    /**
     * ATR значение (волатильность)
     */
    @Column(name = "atr_value", precision = 18, scale = 8)
    private BigDecimal atrValue;

    /**
     * Сигнал технического анализа
     */
    @Column(name = "signal_strength", precision = 8, scale = 4)
    private BigDecimal signalStrength;

    /**
     * Дополнительные метаданные в JSON формате
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * Комментарий к сделке
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Версия записи (для оптимистичной блокировки)
     */
    @Version
    private Long version;

    /**
     * Автоматическое обновление временной метки
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Автоматическая установка времени создания
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        // Устанавливаем значения по умолчанию
        if (this.isScalping == null) {
            this.isScalping = true;
        }
        if (this.executedQuantity == null) {
            this.executedQuantity = BigDecimal.ZERO;
        }
        if (this.commission == null) {
            this.commission = BigDecimal.ZERO;
        }
    }

    /**
     * Проверить, является ли сделка прибыльной
     *
     * @return true если прибыльная
     */
    public boolean isProfitable() {
        return realizedPnl != null && realizedPnl.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Проверить, является ли сделка убыточной
     *
     * @return true если убыточная
     */
    public boolean isLoss() {
        return realizedPnl != null && realizedPnl.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Проверить, полностью ли исполнен ордер
     *
     * @return true если полностью исполнен
     */
    public boolean isFullyExecuted() {
        return executedQuantity != null && quantity != null &&
                executedQuantity.compareTo(quantity) >= 0;
    }

    /**
     * Проверить, частично ли исполнен ордер
     *
     * @return true если частично исполнен
     */
    public boolean isPartiallyExecuted() {
        return executedQuantity != null && executedQuantity.compareTo(BigDecimal.ZERO) > 0 &&
                (quantity == null || executedQuantity.compareTo(quantity) < 0);
    }

    /**
     * Проверить, является ли ордер активным
     *
     * @return true если активен
     */
    public boolean isActive() {
        return status == OrderStatus.SUBMITTED || status == OrderStatus.PARTIALLY_FILLED;
    }

    /**
     * Проверить, завершен ли ордер
     *
     * @return true если завершен
     */
    public boolean isCompleted() {
        return status == OrderStatus.FILLED || status == OrderStatus.CANCELLED ||
                status == OrderStatus.REJECTED || status == OrderStatus.FAILED ||
                status == OrderStatus.EXPIRED;
    }

    /**
     * Получить процент исполнения ордера
     *
     * @return процент исполнения (0-100)
     */
    public BigDecimal getExecutionPercent() {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        if (executedQuantity == null) {
            return BigDecimal.ZERO;
        }

        return executedQuantity.divide(quantity, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    /**
     * Получить эффективную цену с учетом комиссий
     *
     * @return эффективная цена
     */
    public BigDecimal getEffectivePrice() {
        if (avgPrice == null || commission == null || executedQuantity == null ||
                executedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return avgPrice;
        }

        // Учитываем комиссию в цене
        BigDecimal commissionPerUnit = commission.divide(executedQuantity, 8, BigDecimal.ROUND_HALF_UP);

        if (orderSide == OrderSide.BUY) {
            return avgPrice.add(commissionPerUnit);
        } else {
            return avgPrice.subtract(commissionPerUnit);
        }
    }

    /**
     * Рассчитать общую стоимость с комиссией
     *
     * @return общая стоимость включая комиссию
     */
    public BigDecimal getTotalCost() {
        BigDecimal baseCost = totalValue != null ? totalValue : BigDecimal.ZERO;
        BigDecimal commissionCost = commission != null ? commission : BigDecimal.ZERO;

        return baseCost.add(commissionCost);
    }

    /**
     * Получить строковое представление для логов
     *
     * @return строковое представление
     */
    @Override
    public String toString() {
        return String.format("Trade{id=%d, pair='%s', %s %s, qty=%.8f, price=%.8f, status=%s, pnl=%.2f}",
                id, tradingPair, orderSide, orderType,
                quantity != null ? quantity : BigDecimal.ZERO,
                avgPrice != null ? avgPrice : BigDecimal.ZERO,
                status,
                realizedPnl != null ? realizedPnl : BigDecimal.ZERO);
    }
}