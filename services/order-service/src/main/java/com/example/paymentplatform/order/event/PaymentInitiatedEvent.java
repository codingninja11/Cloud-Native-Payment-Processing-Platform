package com.example.paymentplatform.order.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentInitiatedEvent(
    @JsonProperty("eventType") String eventType,
    @JsonProperty("paymentId") String paymentId,
    @JsonProperty("orderId") String orderId,
    @JsonProperty("userId") String userId,
    @JsonProperty("amount") BigDecimal amount,
    @JsonProperty("currency") String currency
) {}
