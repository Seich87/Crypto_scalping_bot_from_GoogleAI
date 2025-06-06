package com.example.scalpingBot.controller;

import com.example.scalpingBot.dto.response.PositionResponse;
import com.example.scalpingBot.dto.response.TradeResponse;
import com.example.scalpingBot.service.market.MarketDataService;
import com.example.scalpingBot.service.trading.PositionManager;
import com.example.scalpingBot.service.trading.TradingQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST-контроллер для управления торговыми операциями и просмотра их состояния.
 * Реализован с разделением ответственности на сервисы чтения (Query) и записи (Command).
 */
@RestController
@RequestMapping("/api/trading")
public class TradingController {

    private final PositionManager positionManager; // Сервис для изменения состояния (команды)
    private final TradingQueryService tradingQueryService; // Сервис для чтения состояния (запросы)
    private final MarketDataService marketDataService; // Нужен для получения цены при ручном закрытии

    @Autowired
    public TradingController(PositionManager positionManager,
                             TradingQueryService tradingQueryService,
                             MarketDataService marketDataService) {
        this.positionManager = positionManager;
        this.tradingQueryService = tradingQueryService;
        this.marketDataService = marketDataService;
    }

    /**
     * Получает список всех активных (открытых) позиций с расчетом нереализованного PnL.
     * @return Список PositionResponse.
     */
    @GetMapping("/positions/active")
    public ResponseEntity<List<PositionResponse>> getActivePositions() {
        return ResponseEntity.ok(tradingQueryService.getActivePositions());
    }

    /**
     * Получает историю всех закрытых позиций для указанной торговой пары.
     * @param symbol Торговая пара, например "BTCUSDT".
     * @return Список PositionResponse.
     */
    @GetMapping("/positions/history")
    public ResponseEntity<List<PositionResponse>> getPositionHistory(@RequestParam String symbol) {
        return ResponseEntity.ok(tradingQueryService.getPositionHistory(symbol));
    }

    /**
     * Получает историю сделок для указанной торговой пары.
     * @param symbol Торговая пара, например "BTCUSDT".
     * @return Список TradeResponse.
     */
    @GetMapping("/trades/history")
    public ResponseEntity<List<TradeResponse>> getTradeHistory(@RequestParam String symbol) {
        return ResponseEntity.ok(tradingQueryService.getTradeHistory(symbol));
    }

    /**
     * Эндпоинт для ручного экстренного закрытия активной позиции по текущей рыночной цене.
     * Используется HTTP метод DELETE, так как операция является идемпотентной и удаляет ресурс (активную позицию).
     *
     * @param symbol Торговая пара, позицию по которой нужно закрыть.
     * @return ResponseEntity без тела с кодом 204 No Content в случае успеха.
     */
    @DeleteMapping("/positions/active")
    public ResponseEntity<Void> manuallyClosePosition(@RequestParam String symbol) {
        // Получаем текущую рыночную цену для закрытия
        var currentTicker = marketDataService.getCurrentTicker(symbol);
        // Вызываем команду на закрытие позиции
        positionManager.closePosition(symbol, currentTicker.getLastPrice());
        // Возвращаем статус 204 No Content, что является стандартной практикой для успешных DELETE-запросов.
        return ResponseEntity.noContent().build();
    }
}