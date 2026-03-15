package com.example.paymentplatform.order.consumer;

import com.example.paymentplatform.order.domain.Order;
import com.example.paymentplatform.order.event.FraudResultEvent;
import com.example.paymentplatform.order.event.PaymentInitiatedEvent;
import com.example.paymentplatform.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final OrderRepository orderRepository;

    public OrderEventConsumer(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @KafkaListener(topics = "payments", groupId = "order-service", containerFactory = "paymentListenerFactory")
    @Transactional
    public void consumePayment(PaymentInitiatedEvent event) {
        if (event == null || !"payment.initiated".equals(event.eventType())) {
            return;
        }
        log.info("Processing payment.initiated for order {}", event.orderId());

        Order order = orderRepository.findByOrderId(event.orderId())
                .orElseGet(() -> {
                    Order newOrder = new Order();
                    newOrder.setOrderId(event.orderId());
                    newOrder.setUserId(event.userId());
                    newOrder.setAmount(event.amount() != null ? event.amount() : BigDecimal.ZERO);
                    newOrder.setCurrency(event.currency() != null ? event.currency() : "USD");
                    newOrder.setStatus("PENDING_PAYMENT");
                    return newOrder;
                });

        order.setPaymentId(event.paymentId());
        if ("CREATED".equals(order.getStatus())) {
            order.setStatus("PENDING_PAYMENT");
        }
        orderRepository.save(order);
    }

    @KafkaListener(topics = "fraud-results", groupId = "order-service", containerFactory = "fraudListenerFactory")
    @Transactional
    public void consumeFraud(FraudResultEvent event) {
        if (event == null || !"fraud.result".equals(event.eventType())) {
            return;
        }
        log.info("Processing fraud.result for order {}", event.orderId());

        orderRepository.findByOrderId(event.orderId()).ifPresent(order -> {
            order.setFraudDecision(event.decision());
            if ("APPROVED".equals(event.decision())) {
                order.setStatus("PAID");
            } else if ("DECLINED".equals(event.decision())) {
                order.setStatus("PAYMENT_FAILED");
            } else {
                order.setStatus("REVIEW");
            }
            orderRepository.save(order);
        });
    }
}
