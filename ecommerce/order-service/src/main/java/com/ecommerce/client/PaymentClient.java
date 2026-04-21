package com.ecommerce.client;

import com.ecommerce.config.FeignClientConfig;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@FeignClient(
    name = "payment-service",
    configuration = FeignClientConfig.class,
    fallback = PaymentClientFallback.class
)
public interface PaymentClient {

    @PostMapping("/api/payments/internal/initiate")
    PaymentResponse initiatePayment(@RequestBody PaymentRequest request);

    @GetMapping("/api/payments/internal/{paymentId}/status")
    PaymentResponse getPaymentStatus(@PathVariable("paymentId") String paymentId);

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    class PaymentRequest {
        private String orderId;
        private String userId;
        private BigDecimal amount;
        private String currency;
        private String paymentMethod;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    class PaymentResponse {
        private String paymentId;
        private String orderId;
        private String status;       // PENDING, SUCCESS, FAILED
        private BigDecimal amount;
        private String transactionId;
        private String message;
    }
}
