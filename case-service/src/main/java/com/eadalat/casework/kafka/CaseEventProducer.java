package com.eadalat.casework.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Publishes domain events for the case lifecycle to the {@code case-events} topic.
 */
@Component
public class CaseEventProducer {

    public static final String TOPIC = "case-events";

    public enum EventType {
        CASE_FILED,
        CASE_ASSIGNED,
        STATUS_CHANGED
    }

    private static final Logger log = LoggerFactory.getLogger(CaseEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public CaseEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(EventType type,
                        Long caseId,
                        String status,
                        Long petitionerId,
                        Long lawyerId,
                        Long judgeId,
                        Long actorId) {
        Map<String, Object> event = new HashMap<>();
        event.put("caseId", caseId);
        event.put("type", type.name());
        event.put("status", status);
        event.put("petitionerId", petitionerId);
        event.put("lawyerId", lawyerId);
        event.put("judgeId", judgeId);
        event.put("actorId", actorId);
        event.put("timestamp", Instant.now().toString());

        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, caseId != null ? caseId.toString() : null, payload);
            log.info("Published {} for caseId={}", type, caseId);
        } catch (Exception e) {
            // Never let an event failure break the primary business transaction.
            log.error("Failed to publish {} for caseId={}: {}", type, caseId, e.getMessage());
        }
    }
}
