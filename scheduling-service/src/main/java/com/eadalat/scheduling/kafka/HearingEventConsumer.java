package com.eadalat.scheduling.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumes cross-service case events that the scheduling service reacts to.
 */
@Component
public class HearingEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(HearingEventConsumer.class);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public HearingEventConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "case-events", groupId = "scheduling-group")
    public void onCaseEvent(String payload) {
        try {
            Map<String, Object> event = objectMapper.readValue(payload, MAP_TYPE);
            Object type = event.get("type");
            if ("CASE_ASSIGNED".equals(type)) {
                log.info("Scheduling placeholder: case {} assigned, auto-suggest pending", event.get("caseId"));
            } else {
                log.debug("Ignoring case event of type {}", type);
            }
        } catch (Exception e) {
            log.error("Failed to process case event", e);
        }
    }
}
