package com.ecommerce.service;

import com.ecommerce.model.NotificationLog;
import com.ecommerce.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final NotificationLogRepository notificationLogRepository;

    private static final Map<String, String> SUBJECT_TEMPLATES = Map.of(
        "order.placed",    "Order Placed Successfully - #{orderId}",
        "order.paid",      "Payment Confirmed - Order #{orderId}",
        "order.cancelled", "Order Cancelled - #{orderId}",
        "order.shipped",   "Your Order Has Shipped - #{orderId}",
        "order.delivered", "Order Delivered - #{orderId}"
    );

    private static final Map<String, String> BODY_TEMPLATES = Map.of(
        "order.placed",
            "Dear Customer,\n\nYour order #{orderId} has been placed successfully.\nTotal Amount: $#{totalAmount}\n\nThank you for shopping with us!\n\nEcommerce Team",
        "order.paid",
            "Dear Customer,\n\nPayment for your order #{orderId} has been confirmed.\nTotal Amount: $#{totalAmount}\n\nYour order is now being processed.\n\nEcommerce Team",
        "order.cancelled",
            "Dear Customer,\n\nYour order #{orderId} has been cancelled.\nIf you didn't request this, please contact support.\n\nEcommerce Team",
        "order.shipped",
            "Dear Customer,\n\nGreat news! Your order #{orderId} has been shipped.\nYou will receive it within 3-5 business days.\n\nEcommerce Team",
        "order.delivered",
            "Dear Customer,\n\nYour order #{orderId} has been delivered.\nThank you for shopping with us!\n\nEcommerce Team"
    );

    public void sendOrderNotification(String eventType, String orderId, String userId,
                                       String recipientEmail, String totalAmount) {
        log.info("Sending {} email notification for order: {} to: {}", eventType, orderId, recipientEmail);

        String subject = SUBJECT_TEMPLATES.getOrDefault(eventType, "Order Update - " + orderId)
                .replace("#{orderId}", orderId);
        String body = BODY_TEMPLATES.getOrDefault(eventType,
                        "Your order " + orderId + " has been updated.")
                .replace("#{orderId}", orderId)
                .replace("#{totalAmount}", totalAmount != null ? totalAmount : "N/A");

        NotificationLog log_ = NotificationLog.builder()
                .userId(userId)
                .eventType(eventType)
                .orderId(orderId)
                .channel("EMAIL")
                .recipient(recipientEmail)
                .content(body)
                .status(NotificationLog.NotificationStatus.SENT)
                .build();

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recipientEmail);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom("noreply@ecommerce.com");
            mailSender.send(message);

            log_.setStatus(NotificationLog.NotificationStatus.SENT);
            log.info("Email sent successfully to: {} for order: {}", recipientEmail, orderId);
        } catch (Exception e) {
            log_.setStatus(NotificationLog.NotificationStatus.FAILED);
            log_.setFailureReason(e.getMessage());
            log.error("Failed to send email to: {} - {}", recipientEmail, e.getMessage());
        } finally {
            notificationLogRepository.save(log_);
        }
    }
}
