package com.example.paymentplatform.payment.event;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentEvent(
    String eventType,
    String eventVersion,
    String paymentId,
    String orderId,
    String userId,
    BigDecimal amount,
    String currency,
    String paymentMethod,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    Instant createdAt,
    String idempotencyKey
) {
    public static PaymentEvent initiated(String paymentId, String orderId, String userId,
                                         BigDecimal amount, String currency, String paymentMethod,
                                         Instant createdAt, String idempotencyKey) {
        return new PaymentEvent(
            "payment.initiated",
            "v1",
            paymentId,
            orderId,
            userId,
            amount,
            currency,
            paymentMethod,
            createdAt,
            idempotencyKey
        );
    }
}
