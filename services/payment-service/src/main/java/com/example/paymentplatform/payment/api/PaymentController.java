package com.example.paymentplatform.payment.api;

import com.example.paymentplatform.payment.domain.Payment;
import com.example.paymentplatform.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<?> createPayment(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody Map<String, Object> payload) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Idempotency-Key header is required"));
        }
        Payment payment = paymentService.createPayment(idempotencyKey, payload);
        return ResponseEntity.status(201).body(Map.of(
                "paymentId", payment.getPaymentId(),
                "orderId", payment.getOrderId(),
                "status", payment.getStatus(),
                "amount", payment.getAmount(),
                "currency", payment.getCurrency(),
                "createdAt", payment.getCreatedAt().toString()
        ));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<Map<String, Object>> getPayment(@PathVariable String paymentId) {
        return paymentService.getByPaymentId(paymentId)
                .map(p -> {
                    Map<String, Object> body = new HashMap<>();
                    body.put("paymentId", p.getPaymentId());
                    body.put("orderId", p.getOrderId());
                    body.put("status", p.getStatus());
                    body.put("amount", p.getAmount());
                    body.put("currency", p.getCurrency());
                    body.put("createdAt", p.getCreatedAt().toString());
                    return ResponseEntity.<Map<String, Object>>ok(body);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/healthz")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
