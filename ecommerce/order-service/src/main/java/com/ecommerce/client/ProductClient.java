package com.ecommerce.client;

import com.ecommerce.config.FeignClientConfig;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(
    name = "product-service",
    configuration = FeignClientConfig.class,
    fallback = ProductClientFallback.class
)
public interface ProductClient {

    @PostMapping("/api/products/internal/batch")
    List<ProductResponse> getProductsByIds(@RequestBody List<String> ids);

    @PatchMapping("/api/products/internal/{id}/stock")
    void updateStock(@PathVariable("id") String productId, @RequestBody StockUpdateRequest request);

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    class ProductResponse {
        private String id;
        private String name;
        private BigDecimal price;
        private Integer stockQuantity;
        private String category;
        private boolean active;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    class StockUpdateRequest {
        private Integer quantity;
        private String operation;
    }
}
