package com.ecommerce.repository;

import com.ecommerce.model.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, String> {
    Page<NotificationLog> findByUserId(String userId, Pageable pageable);
    Page<NotificationLog> findByOrderId(String orderId, Pageable pageable);
}
