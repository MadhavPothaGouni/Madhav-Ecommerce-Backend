package com.ecommerce.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentClientFallback implements PaymentClient {

    @Override
    public PaymentResponse initiatePayment(PaymentRequest request) {
        log.error("Fallback: payment-service unavailable for order: {}", request.getOrderId());
        return PaymentResponse.builder()
                .orderId(request.getOrderId())
                .status("FAILED")
                .message("Payment service temporarily unavailable")
                .build();
    }

    @Override
    public PaymentResponse getPaymentStatus(String paymentId) {
        log.error("Fallback: payment-service unavailable to check status for payment: {}", paymentId);
        return PaymentResponse.builder()
                .paymentId(paymentId)
                .status("UNKNOWN")
                .message("Payment service temporarily unavailable")
                .build();
    }
}
