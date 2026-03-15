package com.example.paymentplatform.fraud.event;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record FraudResultEvent(
    String eventType,
    String eventVersion,
    String paymentId,
    String orderId,
    String userId,
    double score,
    String decision,
    String reason,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    Instant evaluatedAt
) {
    public static FraudResultEvent of(String paymentId, String orderId, String userId,
                                      double score, String decision, String reason) {
        return new FraudResultEvent(
            "fraud.result",
            "v1",
            paymentId,
            orderId,
            userId,
            score,
            decision,
            reason,
            Instant.now()
        );
    }
}
