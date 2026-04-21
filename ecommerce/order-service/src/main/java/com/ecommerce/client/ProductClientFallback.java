package com.ecommerce.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class ProductClientFallback implements ProductClient {

    @Override
    public List<ProductResponse> getProductsByIds(List<String> ids) {
        log.error("Fallback triggered: product-service unavailable for batch fetch of IDs: {}", ids);
        return Collections.emptyList();
    }

    @Override
    public void updateStock(String productId, StockUpdateRequest request) {
        log.error("Fallback triggered: product-service unavailable for stock update on product: {}", productId);
        throw new RuntimeException("Product service unavailable, cannot update stock");
    }
}
