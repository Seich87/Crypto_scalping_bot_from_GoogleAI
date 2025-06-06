package com.example.scalpingBot.controller;

import com.example.scalpingBot.dto.request.TradeRequest;
import com.example.scalpingBot.dto.response.PositionResponse;
import com.example.scalpingBot.dto.response.TradeResponse;
import com.example.scalpingBot.entity.Position;
import com.example.scalpingBot.entity.Trade;
import com.example.scalpingBot.service.trading.PositionManager;
import com.example.scalpingBot.service.trading.TradingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * REST контроллер для управления торговыми операциями
 *
 * Предоставляет API для:
 * - Запуска/остановки торговой стратегии
 * - Просмотра активных позиций
 * - Ручного открытия/закрытия позиций
 * - Получения торговой статистики
 * - Аварийной остановки торговли
 *
 * @author ScalpingBot Team
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/trading")
@RequiredArgsConstructor
@Validated
public class TradingController {

    private final TradingService tradingService;
    private final PositionManager positionManager;

    /**
     * Запустить торговый цикл вручную
     *
     * @return результат запуска
     */
    @PostMapping("/start")
    public ResponseEntity<String> startTrading() {
        log.info("Manual trading cycle start requested");

        try {
            CompletableFuture<Void> result = tradingService.executeScalpingCycle();
            return ResponseEntity.ok("Trading cycle started successfully");
        } catch (Exception e) {
            log.error("Failed to start trading cycle: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to start trading: " + e.getMessage());
        }
    }

    /**
     * Аварийная остановка всей торговли
     *
     * @param reason причина остановки
     * @return результат остановки
     */
    @PostMapping("/emergency-stop")
    public ResponseEntity<String> emergencyStop(@RequestParam String reason) {
        log.warn("EMERGENCY STOP requested: {}", reason);

        try {
            tradingService.emergencyStop(reason);
            return ResponseEntity.ok("Emergency stop executed. All positions closed.");
        } catch (Exception e) {
            log.error("Failed to execute emergency stop: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Emergency stop failed: " + e.getMessage());
        }
    }

    /**
     * Получить все активные позиции
     *
     * @return список активных позиций
     */
    @GetMapping("/positions")
    public ResponseEntity<List<PositionResponse>> getActivePositions() {
        try {
            List<Position> positions = positionManager.getActivePositions();
            List<PositionResponse> response = positions.stream()
                    .map(this::convertToPositionResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get active positions: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Получить позицию по ID
     *
     * @param positionId ID позиции
     * @return позиция
     */
    @GetMapping("/positions/{positionId}")
    public ResponseEntity<PositionResponse> getPosition(@PathVariable Long positionId) {
        // Здесь должна быть логика получения позиции по ID
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /**
     * Закрыть позицию вручную
     *
     * @param positionId ID позиции
     * @param reason причина закрытия
     * @return результат закрытия
     */
    @PostMapping("/positions/{positionId}/close")
    public ResponseEntity<String> closePosition(
            @PathVariable Long positionId,
            @RequestParam String reason) {

        log.info("Manual position close requested for position {}: {}", positionId, reason);

        // Здесь должна быть логика закрытия позиции
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body("Position closing not implemented yet");
    }

    /**
     * Получить торговую статистику за сегодня
     *
     * @return статистика торговли
     */
    @GetMapping("/statistics/today")
    public ResponseEntity<TradingService.TradingStatistics> getTodayStatistics() {
        try {
            TradingService.TradingStatistics stats = tradingService.getTodayStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Failed to get trading statistics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Получить статистику позиций
     *
     * @return статистика позиций
     */
    @GetMapping("/positions/statistics")
    public ResponseEntity<PositionManager.PositionStatistics> getPositionStatistics() {
        try {
            PositionManager.PositionStatistics stats = positionManager.getPositionStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Failed to get position statistics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Создать ручной ордер (для тестирования)
     *
     * @param request параметры ордера
     * @return созданный ордер
     */
    @PostMapping("/orders")
    public ResponseEntity<TradeResponse> createManualOrder(@Valid @RequestBody TradeRequest request) {
        log.info("Manual order creation requested: {} {} {}",
                request.getSide(), request.getQuantity(), request.getTradingPair());

        // Здесь должна быть логика создания ручного ордера
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /**
     * Получить список последних сделок
     *
     * @param limit количество сделок
     * @return список сделок
     */
    @GetMapping("/trades")
    public ResponseEntity<List<TradeResponse>> getRecentTrades(
            @RequestParam(defaultValue = "20") int limit) {

        // Здесь должна быть логика получения последних сделок
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /**
     * Обновить P&L всех позиций
     *
     * @return результат обновления
     */
    @PostMapping("/positions/update-pnl")
    public ResponseEntity<String> updatePositionsPnL() {
        try {
            CompletableFuture<Void> result = positionManager.updateAllPositionsPnL();
            return ResponseEntity.ok("P&L update initiated for all positions");
        } catch (Exception e) {
            log.error("Failed to update positions P&L: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update P&L: " + e.getMessage());
        }
    }

    /**
     * Закрыть все позиции
     *
     * @param reason причина закрытия
     * @return количество закрытых позиций
     */
    @PostMapping("/positions/close-all")
    public ResponseEntity<String> closeAllPositions(@RequestParam String reason) {
        log.warn("Close all positions requested: {}", reason);

        try {
            int closedCount = positionManager.closeAllPositions(reason);
            return ResponseEntity.ok(String.format("Closed %d positions", closedCount));
        } catch (Exception e) {
            log.error("Failed to close all positions: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to close positions: " + e.getMessage());
        }
    }

    /**
     * Конвертировать Position в PositionResponse
     *
     * @param position позиция
     * @return DTO ответа
     */
    private PositionResponse convertToPositionResponse(Position position) {
        return PositionResponse.builder()
                .id(position.getId())
                .tradingPair(position.getTradingPair())
                .side(position.getSide())
                .status(position.getStatus())
                .size(position.getSize())
                .entryPrice(position.getEntryPrice())
                .currentPrice(position.getCurrentPrice())
                .unrealizedPnl(position.getUnrealizedPnl())
                .unrealizedPnlPercent(position.getUnrealizedPnlPercent())
                .realizedPnl(position.getRealizedPnl())
                .stopLossPrice(position.getStopLossPrice())
                .takeProfitPrice(position.getTakeProfitPrice())
                .riskLevel(position.getRiskLevel())
                .holdingTimeMinutes(position.getHoldingTimeMinutes())
                .openedAt(position.getOpenedAt())
                .build();
    }

    /**
     * Конвертировать Trade в TradeResponse
     *
     * @param trade сделка
     * @return DTO ответа
     */
    private TradeResponse convertToTradeResponse(Trade trade) {
        return TradeResponse.builder()
                .id(trade.getId())
                .tradingPair(trade.getTradingPair())
                .orderType(trade.getOrderType())
                .orderSide(trade.getOrderSide())
                .status(trade.getStatus())
                .quantity(trade.getQuantity())
                .executedQuantity(trade.getExecutedQuantity())
                .price(trade.getPrice())
                .avgPrice(trade.getAvgPrice())
                .totalValue(trade.getTotalValue())
                .commission(trade.getCommission())
                .realizedPnl(trade.getRealizedPnl())
                .executedAt(trade.getExecutedAt())
                .build();
    }
}