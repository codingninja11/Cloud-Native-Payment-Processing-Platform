package com.example.paymentplatform.payment.service;

import com.example.paymentplatform.payment.domain.Payment;
import com.example.paymentplatform.payment.event.PaymentEvent;
import com.example.paymentplatform.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private static final String TOPIC_PAYMENTS = "payments";

    private final PaymentRepository paymentRepository;
    private final Optional<KafkaTemplate<String, PaymentEvent>> kafkaTemplate;

    public PaymentService(PaymentRepository paymentRepository,
                          @Autowired(required = false) KafkaTemplate<String, PaymentEvent> kafkaTemplate) {
        this.paymentRepository = paymentRepository;
        this.kafkaTemplate = Optional.ofNullable(kafkaTemplate);
    }

    @Transactional
    public Payment createPayment(String idempotencyKey, Map<String, Object> payload) {
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        String orderId = (String) payload.getOrDefault("orderId", "");
        String userId = (String) payload.getOrDefault("userId", "");
        BigDecimal amount = toBigDecimal(payload.get("amount"));
        String currency = (String) payload.getOrDefault("currency", "USD");
        String paymentMethod = (String) payload.getOrDefault("paymentMethod", "CARD");
        String cardLast4 = payload.get("cardLast4") != null ? payload.get("cardLast4").toString() : null;

        String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8);

        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setOrderId(orderId);
        payment.setUserId(userId);
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setPaymentMethod(paymentMethod);
        payment.setCardLast4(cardLast4);
        payment.setStatus("PENDING");
        payment.setIdempotencyKey(idempotencyKey);

        payment = paymentRepository.save(payment);

        // Publish after DB commit so a down Kafka broker cannot roll back the transaction.
        final Payment committed = payment;
        kafkaTemplate.ifPresent(kt -> {
            PaymentEvent event = PaymentEvent.initiated(
                committed.getPaymentId(),
                committed.getOrderId(),
                committed.getUserId(),
                committed.getAmount(),
                committed.getCurrency(),
                committed.getPaymentMethod(),
                committed.getCreatedAt(),
                committed.getIdempotencyKey()
            );
            Runnable send = () -> kt.send(TOPIC_PAYMENTS, committed.getPaymentId(), event);
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        send.run();
                    }
                });
            } else {
                send.run();
            }
        });

        return payment;
    }

    public Optional<Payment> getByPaymentId(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId);
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(value.toString());
    }
}
