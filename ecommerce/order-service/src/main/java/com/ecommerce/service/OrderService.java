package com.ecommerce.service;

import com.ecommerce.client.PaymentClient;
import com.ecommerce.client.ProductClient;
import com.ecommerce.config.RabbitMQConfig;
import com.ecommerce.dto.OrderDto;
import com.ecommerce.exception.OrderNotFoundException;
import com.ecommerce.exception.OrderProcessingException;
import com.ecommerce.model.Order;
import com.ecommerce.model.OrderItem;
import com.ecommerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final PaymentClient paymentClient;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public OrderDto.Response createOrder(OrderDto.CreateRequest request, String userId) {
        log.info("Creating order for user: {} with {} items", userId, request.getItems().size());

        // 1. Fetch product details via Feign
        List<String> productIds = request.getItems().stream()
                .map(OrderDto.OrderItemRequest::getProductId)
                .collect(Collectors.toList());

        List<ProductClient.ProductResponse> products = productClient.getProductsByIds(productIds);
        if (products.isEmpty()) {
            throw new OrderProcessingException("No valid products found for the given product IDs");
        }

        Map<String, ProductClient.ProductResponse> productMap = products.stream()
                .collect(Collectors.toMap(ProductClient.ProductResponse::getId, p -> p));

        // 2. Build order items and calculate total
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new java.util.ArrayList<>();

        for (OrderDto.OrderItemRequest itemReq : request.getItems()) {
            ProductClient.ProductResponse product = productMap.get(itemReq.getProductId());
            if (product == null) {
                throw new OrderProcessingException("Product not found: " + itemReq.getProductId());
            }
            if (!product.isActive()) {
                throw new OrderProcessingException("Product is not available: " + product.getName());
            }
            if (product.getStockQuantity() < itemReq.getQuantity()) {
                throw new OrderProcessingException("Insufficient stock for product: " + product.getName()
                        + ". Available: " + product.getStockQuantity());
            }

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            orderItems.add(OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(product.getPrice())
                    .subtotal(subtotal)
                    .build());
        }

        // 3. Persist the order
        Order order = Order.builder()
                .userId(userId)
                .status(Order.OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .shippingAddress(request.getShippingAddress())
                .build();
        order = orderRepository.save(order);

        // Set back-reference for each item
        final Order savedOrder = order;
        orderItems.forEach(item -> item.setOrder(savedOrder));
        savedOrder.setItems(orderItems);
        order = orderRepository.save(savedOrder);

        log.info("Order {} created with total amount: {}", order.getId(), totalAmount);

        // 4. Deduct stock for each product via Feign
        for (OrderDto.OrderItemRequest itemReq : request.getItems()) {
            productClient.updateStock(itemReq.getProductId(),
                    new ProductClient.StockUpdateRequest(itemReq.getQuantity(), "DEDUCT"));
        }

        // 5. Initiate payment via Feign
        order.setStatus(Order.OrderStatus.PAYMENT_PROCESSING);
        order = orderRepository.save(order);

        PaymentClient.PaymentResponse paymentResponse = paymentClient.initiatePayment(
                PaymentClient.PaymentRequest.builder()
                        .orderId(order.getId())
                        .userId(userId)
                        .amount(totalAmount)
                        .currency("USD")
                        .paymentMethod("CARD")
                        .build()
        );

        // 6. Update order based on payment response
        if ("SUCCESS".equals(paymentResponse.getStatus())) {
            order.setStatus(Order.OrderStatus.PAID);
            order.setPaymentId(paymentResponse.getPaymentId());
            log.info("Payment successful for order: {}", order.getId());
        } else {
            order.setStatus(Order.OrderStatus.PAYMENT_FAILED);
            log.warn("Payment failed for order: {} - {}", order.getId(), paymentResponse.getMessage());
        }
        order = orderRepository.save(order);

        // 7. Publish event to RabbitMQ for notification
        publishOrderEvent(order, RabbitMQConfig.ORDER_PLACED_KEY);

        return toResponse(order);
    }

    public OrderDto.Response getOrderById(String orderId, String userId, String userRole) {
        Order order;
        if ("ROLE_ADMIN".equals(userRole)) {
            order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        } else {
            order = orderRepository.findByIdAndUserId(orderId, userId)
                    .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        }
        return toResponse(order);
    }

    public Page<OrderDto.Response> getUserOrders(String userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable).map(this::toResponse);
    }

    public Page<OrderDto.Response> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public OrderDto.Response cancelOrder(String orderId, String userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        if (order.getStatus() == Order.OrderStatus.SHIPPED ||
            order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new OrderProcessingException("Cannot cancel order in status: " + order.getStatus());
        }

        // Restore stock for each item
        for (OrderItem item : order.getItems()) {
            productClient.updateStock(item.getProductId(),
                    new ProductClient.StockUpdateRequest(item.getQuantity(), "ADD"));
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        order = orderRepository.save(order);
        log.info("Order {} cancelled by user: {}", orderId, userId);

        publishOrderEvent(order, RabbitMQConfig.ORDER_CANCELLED_KEY);
        return toResponse(order);
    }

    @Transactional
    public OrderDto.Response updateOrderStatus(String orderId, Order.OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        order.setStatus(newStatus);
        order = orderRepository.save(order);
        log.info("Order {} status updated to: {}", orderId, newStatus);

        if (newStatus == Order.OrderStatus.PAID) {
            publishOrderEvent(order, RabbitMQConfig.ORDER_PAID_KEY);
        }
        return toResponse(order);
    }

    private void publishOrderEvent(Order order, String routingKey) {
        try {
            Map<String, Object> event = Map.of(
                    "orderId", order.getId(),
                    "userId", order.getUserId(),
                    "status", order.getStatus().name(),
                    "totalAmount", order.getTotalAmount(),
                    "timestamp", java.time.Instant.now().toString()
            );
            rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, routingKey, event);
            log.info("Published event '{}' for order: {}", routingKey, order.getId());
        } catch (Exception e) {
            log.error("Failed to publish event for order {}: {}", order.getId(), e.getMessage());
        }
    }

    private OrderDto.Response toResponse(Order order) {
        List<OrderDto.OrderItemResponse> itemResponses = order.getItems() == null
                ? List.of()
                : order.getItems().stream().map(item -> OrderDto.OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getSubtotal())
                        .build()).collect(Collectors.toList());

        return OrderDto.Response.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .paymentId(order.getPaymentId())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
