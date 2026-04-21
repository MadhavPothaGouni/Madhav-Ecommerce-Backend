package com.ecommerce.service;

import com.ecommerce.dto.ProductDto;
import com.ecommerce.exception.InsufficientStockException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.Product;
import com.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductDto.Response createProduct(ProductDto.CreateRequest request, String sellerId) {
        log.info("Creating product: {} for seller: {}", request.getName(), sellerId);
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .category(request.getCategory())
                .imageUrl(request.getImageUrl())
                .sku(request.getSku())
                .sellerId(sellerId)
                .active(true)
                .build();
        return toResponse(productRepository.save(product));
    }

    @Cacheable(value = "products", key = "#id")
    public ProductDto.Response getProductById(String id) {
        log.debug("Fetching product: {}", id);
        return toResponse(findProductById(id));
    }

    public Page<ProductDto.Response> getAllProducts(Pageable pageable) {
        return productRepository.findByActiveTrue(pageable).map(this::toResponse);
    }

    public Page<ProductDto.Response> getProductsByCategory(String category, Pageable pageable) {
        return productRepository.findByCategoryAndActiveTrue(category, pageable).map(this::toResponse);
    }

    public Page<ProductDto.Response> searchProducts(String keyword, Pageable pageable) {
        return productRepository.searchByKeyword(keyword, pageable).map(this::toResponse);
    }

    public Page<ProductDto.Response> getProductsBySeller(String sellerId, Pageable pageable) {
        return productRepository.findBySellerIdAndActiveTrue(sellerId, pageable).map(this::toResponse);
    }

    public List<ProductDto.Response> getProductsByIds(List<String> ids) {
        return productRepository.findByIdInAndActiveTrue(ids)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public ProductDto.Response updateProduct(String id, ProductDto.CreateRequest request, String sellerId) {
        Product product = findProductById(id);
        if (!product.getSellerId().equals(sellerId)) {
            throw new SecurityException("You are not authorized to update this product");
        }
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(request.getCategory());
        product.setImageUrl(request.getImageUrl());
        product.setSku(request.getSku());
        log.info("Product {} updated", id);
        return toResponse(productRepository.save(product));
    }

    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public void updateStock(String id, ProductDto.StockUpdateRequest request) {
        Product product = findProductById(id);
        if ("ADD".equalsIgnoreCase(request.getOperation())) {
            product.setStockQuantity(product.getStockQuantity() + request.getQuantity());
        } else if ("DEDUCT".equalsIgnoreCase(request.getOperation())) {
            if (product.getStockQuantity() < request.getQuantity()) {
                throw new InsufficientStockException(
                    "Insufficient stock for product: " + id + ". Available: " + product.getStockQuantity());
            }
            product.setStockQuantity(product.getStockQuantity() - request.getQuantity());
        }
        productRepository.save(product);
        log.info("Stock updated for product {}: {} {}", id, request.getOperation(), request.getQuantity());
    }

    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public void deleteProduct(String id, String sellerId) {
        Product product = findProductById(id);
        if (!product.getSellerId().equals(sellerId)) {
            throw new SecurityException("You are not authorized to delete this product");
        }
        product.setActive(false);
        productRepository.save(product);
        log.info("Product {} soft-deleted", id);
    }

    private Product findProductById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private ProductDto.Response toResponse(Product p) {
        return ProductDto.Response.builder()
                .id(p.getId()).name(p.getName()).description(p.getDescription())
                .price(p.getPrice()).stockQuantity(p.getStockQuantity())
                .category(p.getCategory()).imageUrl(p.getImageUrl()).sku(p.getSku())
                .active(p.isActive()).sellerId(p.getSellerId())
                .createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt())
                .build();
    }
}
