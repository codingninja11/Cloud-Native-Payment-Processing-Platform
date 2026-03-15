package com.example.paymentplatform.fraud.consumer;

import com.example.paymentplatform.fraud.event.FraudResultEvent;
import com.example.paymentplatform.fraud.event.PaymentInitiatedEvent;
import com.example.paymentplatform.fraud.service.FraudEvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!local")
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);
    private static final String TOPIC_FRAUD_RESULTS = "fraud-results";

    private final FraudEvaluationService fraudService;
    private final KafkaTemplate<String, FraudResultEvent> kafkaTemplate;

    public PaymentEventConsumer(FraudEvaluationService fraudService,
                                KafkaTemplate<String, FraudResultEvent> kafkaTemplate) {
        this.fraudService = fraudService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "payments", groupId = "fraud-service")
    public void consume(PaymentInitiatedEvent event) {
        if (event == null || !"payment.initiated".equals(event.eventType())) {
            return;
        }
        log.info("Evaluating fraud for payment {}", event.paymentId());

        FraudResultEvent result = fraudService.evaluate(event);
        kafkaTemplate.send(TOPIC_FRAUD_RESULTS, event.paymentId(), result);
        log.info("Fraud result for {}: {}", event.paymentId(), result.decision());
    }
}
