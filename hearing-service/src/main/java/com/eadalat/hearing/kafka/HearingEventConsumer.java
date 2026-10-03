package com.eadalat.hearing.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class HearingEventConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "hearing-events", groupId = "hearing-group")
    public void onHearingEvent(ConsumerRecord<String, String> record) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(record.value(), Map.class);
            String roomId = String.valueOf(payload.getOrDefault("roomId", "unknown"));
            String caseId = String.valueOf(payload.getOrDefault("caseId", "unknown"));
            log.info("Hearing room ready: roomId={} caseId={}", roomId, caseId);
        } catch (Exception e) {
            log.warn("Could not parse hearing event from {}: {}", record.topic(), e.getMessage());
            throw new RuntimeException("Failed to process hearing event", e);
        }
    }
}
