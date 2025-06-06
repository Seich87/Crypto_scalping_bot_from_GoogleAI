package com.example.scalpingBot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

/**
 * Контроллер для проверки "здоровья" (health check) приложения.
 * Предоставляет простой эндпоинт для систем мониторинга.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    /**
     * Возвращает статус приложения. Если этот эндпоинт отвечает,
     * значит, веб-сервер приложения запущен и работает.
     *
     * @return ResponseEntity с JSON-объектом {"status": "UP"} и HTTP-статусом 200 OK.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getHealthStatus() {
        // Collections.singletonMap создает неизменяемую карту с одной записью.
        // Это простой и эффективный способ вернуть простой JSON.
        Map<String, String> response = Collections.singletonMap("status", "UP");
        return ResponseEntity.ok(response);
    }
}