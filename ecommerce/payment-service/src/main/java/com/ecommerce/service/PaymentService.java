package com.ecommerce.service;

import com.ecommerce.dto.PaymentDto;
import com.ecommerce.exception.PaymentNotFoundException;
import com.ecommerce.exception.PaymentProcessingException;
import com.ecommerce.model.Payment;
import com.ecommerce.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentDto.Response initiatePayment(PaymentDto.InitiateRequest request) {
        log.info("Initiating payment for order: {} amount: {} {}",
                request.getOrderId(), request.getAmount(), request.getCurrency());

        // Ensure idempotency — don't re-process an existing payment
        paymentRepository.findByOrderId(request.getOrderId()).ifPresent(existing -> {
            if (existing.getStatus() == Payment.PaymentStatus.SUCCESS) {
                throw new PaymentProcessingException(
                        "Payment already processed for order: " + request.getOrderId());
            }
        });

        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .userId(request.getUserId())
                .status(Payment.PaymentStatus.PROCESSING)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .build();
        payment = paymentRepository.save(payment);

        // === Simulated payment gateway call ===
        // In production, integrate with Stripe / Razorpay / PayPal here
        PaymentGatewayResult gatewayResult = simulateGatewayCall(payment);

        if (gatewayResult.success()) {
            payment.setStatus(Payment.PaymentStatus.SUCCESS);
            payment.setTransactionId(gatewayResult.transactionId());
            payment.setGatewayResponse("{\"status\":\"captured\",\"txn\":\"" + gatewayResult.transactionId() + "\"}");
            payment.setProcessedAt(LocalDateTime.now());
            log.info("Payment SUCCESS for order: {} txn: {}", request.getOrderId(), gatewayResult.transactionId());
        } else {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailureReason(gatewayResult.failureReason());
            log.warn("Payment FAILED for order: {} reason: {}", request.getOrderId(), gatewayResult.failureReason());
        }

        payment = paymentRepository.save(payment);
        return toResponse(payment, gatewayResult.success() ? "Payment processed successfully" : gatewayResult.failureReason());
    }

    public PaymentDto.Response getPaymentStatus(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));
        return toResponse(payment, null);
    }

    public PaymentDto.Response getPaymentByOrderId(String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for order: " + orderId));
        return toResponse(payment, null);
    }

    public Page<PaymentDto.Response> getUserPayments(String userId, Pageable pageable) {
        return paymentRepository.findByUserId(userId, pageable).map(p -> toResponse(p, null));
    }

    @Transactional
    public PaymentDto.Response refundPayment(String paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));

        if (payment.getStatus() != Payment.PaymentStatus.SUCCESS) {
            throw new PaymentProcessingException("Cannot refund payment in status: " + payment.getStatus());
        }

        // Simulate refund gateway call
        log.info("Processing refund for payment: {} reason: {}", paymentId, reason);
        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        payment.setFailureReason("Refunded: " + reason);
        payment = paymentRepository.save(payment);

        return toResponse(payment, "Refund processed successfully");
    }

    // ── Simulated Gateway ──────────────────────────────────────────────────────
    private PaymentGatewayResult simulateGatewayCall(Payment payment) {
        // Simulate 90% success rate; amount > 9999 forces failure for demo
        if (payment.getAmount().doubleValue() > 9999) {
            return new PaymentGatewayResult(false, null, "Amount exceeds limit");
        }
        boolean success = Math.random() > 0.1;
        if (success) {
            return new PaymentGatewayResult(true, "TXN-" + UUID.randomUUID().toString().toUpperCase().substring(0, 8), null);
        }
        return new PaymentGatewayResult(false, null, "Card declined by issuing bank");
    }

    private record PaymentGatewayResult(boolean success, String transactionId, String failureReason) {}

    private PaymentDto.Response toResponse(Payment p, String message) {
        return PaymentDto.Response.builder()
                .paymentId(p.getId())
                .orderId(p.getOrderId())
                .userId(p.getUserId())
                .status(p.getStatus())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .paymentMethod(p.getPaymentMethod())
                .transactionId(p.getTransactionId())
                .message(message != null ? message : p.getStatus().name())
                .createdAt(p.getCreatedAt())
                .processedAt(p.getProcessedAt())
                .build();
    }
}
