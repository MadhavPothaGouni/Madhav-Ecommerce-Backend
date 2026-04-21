package com.ecommerce.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/user-service")
    public ResponseEntity<Map<String, Object>> userServiceFallback() {
        log.warn("Circuit breaker opened for user-service");
        return fallbackResponse("user-service");
    }

    @GetMapping("/product-service")
    public ResponseEntity<Map<String, Object>> productServiceFallback() {
        log.warn("Circuit breaker opened for product-service");
        return fallbackResponse("product-service");
    }

    @GetMapping("/order-service")
    public ResponseEntity<Map<String, Object>> orderServiceFallback() {
        log.warn("Circuit breaker opened for order-service");
        return fallbackResponse("order-service");
    }

    @GetMapping("/payment-service")
    public ResponseEntity<Map<String, Object>> paymentServiceFallback() {
        log.warn("Circuit breaker opened for payment-service");
        return fallbackResponse("payment-service");
    }

    private ResponseEntity<Map<String, Object>> fallbackResponse(String service) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "error", "Service temporarily unavailable",
                "service", service,
                "message", "The service is currently unavailable. Please try again later.",
                "timestamp", Instant.now().toString()
        ));
    }
}
