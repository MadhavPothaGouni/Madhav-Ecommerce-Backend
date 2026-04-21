package com.ecommerce.dto;

import com.ecommerce.model.Order;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDto {

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CreateRequest {
        @NotEmpty @Valid
        private List<OrderItemRequest> items;

        @NotBlank
        private String shippingAddress;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class OrderItemRequest {
        @NotBlank private String productId;
        @NotNull @Min(1) private Integer quantity;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private String id;
        private String userId;
        private Order.OrderStatus status;
        private BigDecimal totalAmount;
        private String shippingAddress;
        private String paymentId;
        private List<OrderItemResponse> items;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OrderItemResponse {
        private String id;
        private String productId;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}
