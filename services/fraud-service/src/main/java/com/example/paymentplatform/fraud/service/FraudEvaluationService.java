package com.example.paymentplatform.fraud.service;

import com.example.paymentplatform.fraud.event.FraudResultEvent;
import com.example.paymentplatform.fraud.event.PaymentInitiatedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class FraudEvaluationService {

    @Value("${fraud.max-amount-without-review:1000}")
    private double maxAmountWithoutReview;

    public FraudResultEvent evaluate(PaymentInitiatedEvent event) {
        BigDecimal amount = event.amount() != null ? event.amount() : BigDecimal.ZERO;
        double amt = amount.doubleValue();

        double score;
        String decision;
        String reason;

        if (amt > maxAmountWithoutReview * 2) {
            score = 0.85;
            decision = "DECLINED";
            reason = "Amount exceeds high-risk threshold";
        } else if (amt > maxAmountWithoutReview) {
            score = 0.45;
            decision = "REVIEW";
            reason = "Amount above auto-approve threshold";
        } else {
            score = 0.15;
            decision = "APPROVED";
            reason = "Below risk threshold";
        }

        return FraudResultEvent.of(
            event.paymentId(),
            event.orderId(),
            event.userId(),
            score,
            decision,
            reason
        );
    }
}
