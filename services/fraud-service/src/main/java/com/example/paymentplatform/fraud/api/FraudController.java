package com.example.paymentplatform.fraud.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/fraud")
public class FraudController {

    @GetMapping("/healthz")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/rules")
    public ResponseEntity<Map<String, Object>> rules() {
        return ResponseEntity.ok(Map.of(
                "strategy", "simple-threshold",
                "maxAmountWithoutReview", 1000,
                "version", "v1"
        ));
    }
}

