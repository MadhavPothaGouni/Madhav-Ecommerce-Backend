package com.ecommerce.controller;

import com.ecommerce.dto.ProductDto;
import com.ecommerce.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductDto.Response> createProduct(
            @Valid @RequestBody ProductDto.CreateRequest request,
            @RequestHeader("X-User-Id") String sellerId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProduct(request, sellerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto.Response> getProduct(@PathVariable String id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/public/all")
    public ResponseEntity<Page<ProductDto.Response>> getAllProducts(Pageable pageable) {
        return ResponseEntity.ok(productService.getAllProducts(pageable));
    }

    @GetMapping("/public/category/{category}")
    public ResponseEntity<Page<ProductDto.Response>> getByCategory(
            @PathVariable String category, Pageable pageable) {
        return ResponseEntity.ok(productService.getProductsByCategory(category, pageable));
    }

    @GetMapping("/public/search")
    public ResponseEntity<Page<ProductDto.Response>> search(
            @RequestParam String keyword, Pageable pageable) {
        return ResponseEntity.ok(productService.searchProducts(keyword, pageable));
    }

    @GetMapping("/my-products")
    public ResponseEntity<Page<ProductDto.Response>> getMyProducts(
            @RequestHeader("X-User-Id") String sellerId, Pageable pageable) {
        return ResponseEntity.ok(productService.getProductsBySeller(sellerId, pageable));
    }

    // Called by order-service via Feign
    @PostMapping("/internal/batch")
    public ResponseEntity<List<ProductDto.Response>> getProductsByIds(@RequestBody List<String> ids) {
        return ResponseEntity.ok(productService.getProductsByIds(ids));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDto.Response> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductDto.CreateRequest request,
            @RequestHeader("X-User-Id") String sellerId) {
        return ResponseEntity.ok(productService.updateProduct(id, request, sellerId));
    }

    // Called by order-service via Feign to deduct stock
    @PatchMapping("/internal/{id}/stock")
    public ResponseEntity<Void> updateStock(
            @PathVariable String id,
            @Valid @RequestBody ProductDto.StockUpdateRequest request) {
        productService.updateStock(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String sellerId) {
        productService.deleteProduct(id, sellerId);
        return ResponseEntity.noContent().build();
    }
}
