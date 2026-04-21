package com.ecommerce.service;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final EmailNotificationService emailService;

    /**
     * Listens to all order.* events from RabbitMQ and dispatches notifications.
     * Uses manual acknowledgement for reliability.
     */
    @RabbitListener(
        queues = "notification.order.queue",
        ackMode = "MANUAL"
    )
    public void handleOrderEvent(
            Map<String, Object> event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            @Header(value = "amqp_receivedRoutingKey", required = false) String routingKey) {

        log.info("Received order event: routingKey={} payload={}", routingKey, event);

        try {
            String orderId     = (String) event.get("orderId");
            String userId      = (String) event.get("userId");
            String status      = (String) event.get("status");
            Object amount      = event.get("totalAmount");
            String totalAmount = amount != null ? amount.toString() : "N/A";

            // Derive the event type from the routing key, e.g. "order.placed"
            String eventType = routingKey != null ? routingKey : "order.updated";

            // In production, fetch user email from user-service or a local cache.
            // Here we use a placeholder for demonstration.
            String recipientEmail = "user_" + userId + "@ecommerce-demo.com";

            emailService.sendOrderNotification(eventType, orderId, userId, recipientEmail, totalAmount);

            // Acknowledge the message only after successful processing
            channel.basicAck(deliveryTag, false);
            log.info("Order event acknowledged: orderId={} eventType={}", orderId, eventType);

        } catch (Exception e) {
            log.error("Error processing order event: {} - {}", event, e.getMessage(), e);
            try {
                // Nack and requeue=false (send to dead-letter queue if configured)
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioEx) {
                log.error("Failed to nack message: {}", ioEx.getMessage());
            }
        }
    }
}
