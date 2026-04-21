package com.ecommerce.controller;

import com.ecommerce.dto.PaymentDto;
import com.ecommerce.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // ── Internal endpoints (called by order-service via Feign) ──────────────
    @PostMapping("/internal/initiate")
    public ResponseEntity<PaymentDto.Response> initiatePayment(
            @Valid @RequestBody PaymentDto.InitiateRequest request) {
        log.info("Internal payment initiation for order: {}", request.getOrderId());
        return ResponseEntity.ok(paymentService.initiatePayment(request));
    }

    @GetMapping("/internal/{paymentId}/status")
    public ResponseEntity<PaymentDto.Response> getPaymentStatus(@PathVariable String paymentId) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(paymentId));
    }

    // ── Public/authenticated endpoints ──────────────────────────────────────
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentDto.Response> getPayment(@PathVariable String paymentId) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(paymentId));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentDto.Response> getPaymentByOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(paymentService.getPaymentByOrderId(orderId));
    }

    @GetMapping("/my-payments")
    public ResponseEntity<Page<PaymentDto.Response>> getMyPayments(
            @RequestHeader("X-User-Id") String userId, Pageable pageable) {
        return ResponseEntity.ok(paymentService.getUserPayments(userId, pageable));
    }

    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<PaymentDto.Response> refundPayment(
            @PathVariable String paymentId,
            @Valid @RequestBody PaymentDto.RefundRequest request) {
        log.info("Refund request for payment: {}", paymentId);
        return ResponseEntity.ok(paymentService.refundPayment(paymentId, request.getReason()));
    }
}
