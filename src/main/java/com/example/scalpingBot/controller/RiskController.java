package com.example.scalpingBot.controller;

import com.example.scalpingBot.dto.response.RiskMetricsResponse;
import com.example.scalpingBot.service.risk.RiskMetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-контроллер для управления и мониторинга параметров риска.
 */
@RestController
@RequestMapping("/api/risk")
public class RiskController {

    private final RiskMetricsService riskMetricsService;

    @Autowired
    public RiskController(RiskMetricsService riskMetricsService) {
        this.riskMetricsService = riskMetricsService;
    }

    /**
     * Получает сводные метрики производительности и рисков бота.
     *
     * @return ResponseEntity с DTO RiskMetricsResponse.
     */
    @GetMapping("/metrics")
    public ResponseEntity<RiskMetricsResponse> getRiskMetrics() {
        RiskMetricsResponse metrics = riskMetricsService.calculateCurrentMetrics();
        return ResponseEntity.ok(metrics);
    }
}