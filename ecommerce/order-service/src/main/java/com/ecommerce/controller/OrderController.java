package com.ecommerce.controller;

import com.ecommerce.dto.OrderDto;
import com.ecommerce.model.Order;
import com.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDto.Response> createOrder(
            @Valid @RequestBody OrderDto.CreateRequest request,
            @RequestHeader("X-User-Id") String userId) {
        log.info("Create order request from user: {}", userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(request, userId));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDto.Response> getOrder(
            @PathVariable String orderId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {
        return ResponseEntity.ok(orderService.getOrderById(orderId, userId, userRole));
    }

    @GetMapping("/my-orders")
    public ResponseEntity<Page<OrderDto.Response>> getMyOrders(
            @RequestHeader("X-User-Id") String userId,
            Pageable pageable) {
        return ResponseEntity.ok(orderService.getUserOrders(userId, pageable));
    }

    @GetMapping("/admin/all")
    public ResponseEntity<Page<OrderDto.Response>> getAllOrders(Pageable pageable) {
        return ResponseEntity.ok(orderService.getAllOrders(pageable));
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<OrderDto.Response> cancelOrder(
            @PathVariable String orderId,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(orderService.cancelOrder(orderId, userId));
    }

    @PatchMapping("/admin/{orderId}/status")
    public ResponseEntity<OrderDto.Response> updateOrderStatus(
            @PathVariable String orderId,
            @RequestParam Order.OrderStatus status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, status));
    }
}
