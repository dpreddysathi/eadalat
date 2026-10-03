package com.eadalat.notify.kafka;

import com.eadalat.notify.service.NotificationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "case-events", groupId = "notification-group")
    public void onCaseEvent(ConsumerRecord<String, String> record) {
        Map<String, Object> payload = parse(record.value());
        if (payload == null) {
            return;
        }
        String type = str(payload, "type");
        Long caseId = num(payload, "caseId");

        switch (type == null ? "" : type) {
            case "CASE_ASSIGNED" -> notifyEach(payload, caseId, "CASE_ASSIGNED",
                    "Your case #" + caseId + " was assigned to a judge.");
            case "STATUS_CHANGED" -> notifyEach(payload, caseId, "STATUS_CHANGED",
                    "Case #" + caseId + " status changed to " + str(payload, "status") + ".");
            case "CASE_FILED" -> {
                Long petitionerId = num(payload, "petitionerId");
                if (petitionerId != null) {
                    notificationService.notify(petitionerId, caseId, "CASE_FILED",
                            "Your case #" + caseId + " was filed successfully.");
                }
            }
            case "DOCUMENT_UPLOADED" -> notifyEach(payload, caseId, "DOCUMENT_UPLOADED",
                    "New document '" + str(payload, "fileName") + "' uploaded to case #" + caseId + ".");
            default -> log.debug("Ignoring unknown case-event type: {}", type);
        }
    }

    @KafkaListener(topics = "hearing-events", groupId = "notification-group")
    public void onHearingEvent(ConsumerRecord<String, String> record) {
        Map<String, Object> payload = parse(record.value());
        if (payload == null) {
            return;
        }
        String type = str(payload, "type");
        Long caseId = num(payload, "caseId");

        String message = switch (type == null ? "" : type) {
            case "HEARING_SCHEDULED" -> {
                StringBuilder sb = new StringBuilder("Hearing scheduled for case #").append(caseId);
                String scheduledAt = str(payload, "scheduledAt");
                String roomId = str(payload, "roomId");
                if (scheduledAt != null) {
                    sb.append(" at ").append(scheduledAt);
                }
                if (roomId != null) {
                    sb.append(" (room ").append(roomId).append(")");
                }
                yield sb.append('.').toString();
            }
            case "HEARING_CANCELLED" -> "Hearing for case #" + caseId + " was cancelled.";
            case "HEARING_COMPLETED" -> "Hearing for case #" + caseId + " completed.";
            default -> null;
        };
        if (message == null) {
            log.debug("Ignoring unknown hearing-event type: {}", type);
            return;
        }
        notifyEach(payload, caseId, type, message);
    }

    /**
     * Notify every recipient id carried by the payload (petitionerId, lawyerId,
     * judgeId, userId). Missing ids are simply skipped, so a payload with no
     * recipient fields results in no notification — nothing to fail on.
     */
    private void notifyEach(Map<String, Object> payload, Long caseId, String type, String message) {
        List<String> recipientKeys = List.of("petitionerId", "lawyerId", "judgeId", "userId");
        boolean any = false;
        for (String key : recipientKeys) {
            Long userId = num(payload, key);
            if (userId != null) {
                notificationService.notify(userId, caseId, type, message);
                any = true;
            }
        }
        if (!any) {
            log.debug("No recipients found in {} event payload; skipping", type);
        }
    }

    private Map<String, Object> parse(String json) {
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception e) {
            log.warn("Skipping unparseable event payload: {}", e.getMessage());
            return null;
        }
    }

    private static String str(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static Long num(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
