package com.ecommerce.repository;

import com.ecommerce.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findByOrderId(String orderId);
    Page<Payment> findByUserId(String userId, Pageable pageable);
    Page<Payment> findByStatus(Payment.PaymentStatus status, Pageable pageable);
}
