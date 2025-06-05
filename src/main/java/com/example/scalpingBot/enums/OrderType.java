package com.example.scalpingBot.enums;

import lombok.Getter;

/**
 * Перечисление типов торговых ордеров для скальпинг-бота
 *
 * Поддерживаемые типы:
 * - MARKET - рыночные ордера для быстрого исполнения
 * - LIMIT - лимитные ордера для точного ценового контроля
 * - STOP_LOSS - стоп-лосс ордера для ограничения потерь
 * - TAKE_PROFIT - тейк-профит ордера для фиксации прибыли
 * - STOP_LIMIT - условные лимитные ордера
 * - OCO - ордера "одна отменяет другую"
 *
 * @author ScalpingBot Team
 * @version 1.0
 */
@Getter
public enum OrderType {

    /**
     * Рыночный ордер - исполняется немедленно по текущей рыночной цене
     *
     * Особенности:
     * - Гарантированное исполнение
     * - Возможен slippage (проскальзывание цены)
     * - Используется для срочного входа/выхода из позиции
     * - Подходит для высоколиквидных инструментов
     */
    MARKET(
            "MARKET",
            "Рыночный ордер",
            true,   // требует немедленного исполнения
            false,  // не требует указания цены
            true    // может быть исполнен частично
    ),

    /**
     * Лимитный ордер - исполняется только по указанной цене или лучше
     *
     * Особенности:
     * - Контроль цены исполнения
     * - Не гарантированное исполнение
     * - Может стоять в стакане длительное время
     * - Подходит для точного ценового контроля
     */
    LIMIT(
            "LIMIT",
            "Лимитный ордер",
            false,  // не требует немедленного исполнения
            true,   // требует указания цены
            true    // может быть исполнен частично
    ),

    /**
     * Стоп-лосс ордер - активируется при достижении стоп-цены
     *
     * Особенности:
     * - Ограничение убытков
     * - Становится рыночным при активации
     * - Критически важен для риск-менеджмента
     * - Должен быть на каждой позиции
     */
    STOP_LOSS(
            "STOP_LOSS",
            "Стоп-лосс ордер",
            false,  // активируется по условию
            true,   // требует указания стоп-цены
            false   // должен исполняться полностью
    ),

    /**
     * Тейк-профит ордер - фиксирует прибыль при достижении целевой цены
     *
     * Особенности:
     * - Фиксация прибыли
     * - Становится лимитным при активации
     * - Используется для автоматического закрытия прибыльных позиций
     * - Часть стратегии скальпинга
     */
    TAKE_PROFIT(
            "TAKE_PROFIT",
            "Тейк-профит ордер",
            false,  // активируется по условию
            true,   // требует указания целевой цены
            false   // должен исполняться полностью
    ),

    /**
     * Стоп-лимит ордер - становится лимитным при достижении стоп-цены
     *
     * Особенности:
     * - Контроль цены при активации стопа
     * - Требует указания стоп-цены и лимит-цены
     * - Может не исполниться при резких движениях
     * - Используется для более точного контроля
     */
    STOP_LIMIT(
            "STOP_LIMIT",
            "Стоп-лимит ордер",
            false,  // активируется по условию
            true,   // требует указания двух цен
            true    // может быть исполнен частично
    ),

    /**
     * OCO ордер - "одна отменяет другую"
     *
     * Особенности:
     * - Комбинация стоп-лосса и тейк-профита
     * - При исполнении одного ордера, второй отменяется
     * - Оптимально для скальпинг стратегии
     * - Автоматизирует управление позицией
     */
    OCO(
            "OCO",
            "Ордер 'одна отменяет другую'",
            false,  // комплексный ордер
            true,   // требует указания двух цен
            false   // специальная логика исполнения
    ),

    /**
     * Trailing Stop - динамический стоп-лосс, следующий за ценой
     *
     * Особенности:
     * - Автоматически корректируется при движении цены в прибыль
     * - Защищает от разворота тренда
     * - Подходит для трендовых движений
     * - Может использоваться в скальпинге при сильных движениях
     */
    TRAILING_STOP(
            "TRAILING_STOP",
            "Скользящий стоп-лосс",
            false,  // динамически обновляется
            true,   // требует указания отступа
            false   // должен исполняться полностью
    );

    /**
     * Код типа ордера для API биржи
     */
    private final String code;

    /**
     * Человекочитаемое описание
     */
    private final String description;

    /**
     * Требует ли немедленного исполнения
     */
    private final boolean requiresImmediateExecution;

    /**
     * Требует ли указания цены
     */
    private final boolean requiresPrice;

    /**
     * Может ли быть исполнен частично
     */
    private final boolean allowsPartialFill;

    /**
     * Конструктор перечисления
     */
    OrderType(String code, String description, boolean requiresImmediateExecution,
              boolean requiresPrice, boolean allowsPartialFill) {
        this.code = code;
        this.description = description;
        this.requiresImmediateExecution = requiresImmediateExecution;
        this.requiresPrice = requiresPrice;
        this.allowsPartialFill = allowsPartialFill;
    }

    /**
     * Найти тип ордера по коду
     *
     * @param code код типа ордера
     * @return найденный тип ордера
     * @throws IllegalArgumentException если код не найден
     */
    public static OrderType fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Order type code cannot be null or empty");
        }

        for (OrderType orderType : values()) {
            if (orderType.code.equalsIgnoreCase(code.trim())) {
                return orderType;
            }
        }

        throw new IllegalArgumentException("Unknown order type code: " + code);
    }

    /**
     * Проверить, является ли ордер стоп-ордером
     *
     * @return true если это стоп-ордер
     */
    public boolean isStopOrder() {
        return this == STOP_LOSS || this == STOP_LIMIT || this == TRAILING_STOP;
    }

    /**
     * Проверить, является ли ордер условным
     *
     * @return true если ордер активируется по условию
     */
    public boolean isConditionalOrder() {
        return this == STOP_LOSS || this == TAKE_PROFIT ||
                this == STOP_LIMIT || this == OCO || this == TRAILING_STOP;
    }

    /**
     * Проверить, подходит ли тип ордера для скальпинга
     *
     * @return true если подходит для скальпинг стратегии
     */
    public boolean isSuitableForScalping() {
        return this == MARKET || this == LIMIT || this == STOP_LOSS ||
                this == TAKE_PROFIT || this == OCO;
    }

    /**
     * Проверить, требует ли ордер мониторинга
     *
     * @return true если требует постоянного мониторинга
     */
    public boolean requiresMonitoring() {
        return isConditionalOrder() || this == LIMIT;
    }

    /**
     * Получить приоритет исполнения (чем меньше число, тем выше приоритет)
     *
     * @return приоритет исполнения
     */
    public int getExecutionPriority() {
        switch (this) {
            case MARKET:
                return 1; // Высший приоритет - немедленное исполнение
            case STOP_LOSS:
                return 2; // Высокий приоритет - защита от потерь
            case TAKE_PROFIT:
                return 3; // Средний приоритет - фиксация прибыли
            case LIMIT:
                return 4; // Обычный приоритет
            case STOP_LIMIT:
            case OCO:
            case TRAILING_STOP:
                return 5; // Низкий приоритет - сложные ордера
            default:
                return 10;
        }
    }

    /**
     * Получить рекомендуемый таймаут для ордера в секундах
     *
     * @return таймаут в секундах
     */
    public int getRecommendedTimeoutSeconds() {
        switch (this) {
            case MARKET:
                return 30;   // Быстрое исполнение
            case STOP_LOSS:
            case TAKE_PROFIT:
                return 60;   // Средний таймаут
            case LIMIT:
                return 300;  // 5 минут для лимитного
            case STOP_LIMIT:
            case OCO:
                return 600;  // 10 минут для сложных
            case TRAILING_STOP:
                return 3600; // 1 час для трейлинга
            default:
                return 300;
        }
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", description, code);
    }
}