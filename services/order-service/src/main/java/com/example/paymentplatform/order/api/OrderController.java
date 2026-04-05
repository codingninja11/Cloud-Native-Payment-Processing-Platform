package com.example.paymentplatform.order.api;

import com.example.paymentplatform.order.domain.Order;
import com.example.paymentplatform.order.repository.OrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orderRepository;

    public OrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Map<String, Object> payload) {
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8);
        String userId = (String) payload.getOrDefault("userId", "unknown");
        BigDecimal amount = toBigDecimal(payload.get("amount"));
        String currency = (String) payload.getOrDefault("currency", "USD");

        Order order = new Order();
        order.setOrderId(orderId);
        order.setUserId(userId);
        order.setAmount(amount);
        order.setCurrency(currency);
        order.setStatus("CREATED");
        order = orderRepository.save(order);

        return ResponseEntity.status(201).body(Map.of(
                "orderId", order.getOrderId(),
                "status", order.getStatus(),
                "amount", order.getAmount(),
                "currency", order.getCurrency(),
                "createdAt", order.getCreatedAt().toString()
        ));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable("orderId") String orderId) {
        return orderRepository.findByOrderId(orderId)
                .map(o -> {
                    Map<String, Object> body = new HashMap<>();
                    body.put("orderId", o.getOrderId());
                    body.put("status", o.getStatus());
                    body.put("paymentId", o.getPaymentId() != null ? o.getPaymentId() : "");
                    body.put("fraudDecision", o.getFraudDecision() != null ? o.getFraudDecision() : "");
                    body.put("amount", o.getAmount());
                    body.put("currency", o.getCurrency());
                    body.put("createdAt", o.getCreatedAt().toString());
                    return ResponseEntity.<Map<String, Object>>ok(body);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/healthz")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(value.toString());
    }
}
