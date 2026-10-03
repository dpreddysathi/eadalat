package com.eadalat.scheduling.service;

import com.eadalat.scheduling.domain.Hearing;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Publishes hearing lifecycle events to the {@code hearing-events} topic.
 */
@Component
public class HearingEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(HearingEventPublisher.class);
    private static final String TOPIC = "hearing-events";

    public static final String TYPE_SCHEDULED = "HEARING_SCHEDULED";
    public static final String TYPE_CANCELLED = "HEARING_CANCELLED";
    public static final String TYPE_COMPLETED = "HEARING_COMPLETED";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public HearingEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(Hearing hearing, String type) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("hearingId", hearing.getId());
        event.put("caseId", hearing.getCaseId());
        event.put("roomId", hearing.getRoomId());
        event.put("type", type);
        event.put("scheduledAt", hearing.getScheduledAt() == null ? null : hearing.getScheduledAt().toString());
        event.put("timestamp", Instant.now().toString());

        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, hearing.getId().toString(), payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish {} for hearing {}", type, hearing.getId(), ex);
                        } else {
                            log.debug("Published {} for hearing {} to {}", type, hearing.getId(), TOPIC);
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize {} event for hearing {}", type, hearing.getId(), e);
        }
    }
}
