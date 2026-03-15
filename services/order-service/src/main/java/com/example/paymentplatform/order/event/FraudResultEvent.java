package com.example.paymentplatform.order.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FraudResultEvent(
    @JsonProperty("eventType") String eventType,
    @JsonProperty("paymentId") String paymentId,
    @JsonProperty("orderId") String orderId,
    @JsonProperty("userId") String userId,
    @JsonProperty("decision") String decision,
    @JsonProperty("reason") String reason
) {}
