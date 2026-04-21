package com.ecommerce.dto;

import com.ecommerce.model.Payment;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentDto {

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class InitiateRequest {
        @NotBlank private String orderId;
        @NotBlank private String userId;
        @NotNull @DecimalMin("0.01") private BigDecimal amount;
        @NotBlank private String currency;
        @NotBlank private String paymentMethod;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private String paymentId;
        private String orderId;
        private String userId;
        private Payment.PaymentStatus status;
        private BigDecimal amount;
        private String currency;
        private String paymentMethod;
        private String transactionId;
        private String message;
        private LocalDateTime createdAt;
        private LocalDateTime processedAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class RefundRequest {
        @NotBlank private String reason;
    }
}
