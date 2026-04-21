package com.ecommerce.controller;

import com.ecommerce.model.NotificationLog;
import com.ecommerce.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationLogRepository notificationLogRepository;

    @GetMapping("/my-notifications")
    public ResponseEntity<Page<NotificationLog>> getMyNotifications(
            @RequestHeader("X-User-Id") String userId,
            Pageable pageable) {
        return ResponseEntity.ok(notificationLogRepository.findByUserId(userId, pageable));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<Page<NotificationLog>> getByOrder(
            @PathVariable String orderId,
            Pageable pageable) {
        return ResponseEntity.ok(notificationLogRepository.findByOrderId(orderId, pageable));
    }
}
