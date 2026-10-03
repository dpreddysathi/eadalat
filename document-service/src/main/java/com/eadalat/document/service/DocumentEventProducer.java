package com.eadalat.document.service;

import com.eadalat.document.entity.Document;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Publishes domain events about documents to the shared {@code case-events} topic.
 */
@Component
@RequiredArgsConstructor
public class DocumentEventProducer {

    private static final String TOPIC = "case-events";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void documentUploaded(Document document) {
        String payload = "{"
                + "\"type\":\"DOCUMENT_UPLOADED\","
                + "\"caseId\":" + document.getCaseId() + ","
                + "\"documentId\":" + document.getId() + ","
                + "\"fileName\":" + json(document.getFileName()) + ","
                + "\"uploadedBy\":" + document.getUploadedBy() + ","
                + "\"timestamp\":" + json(Instant.now().toString())
                + "}";
        kafkaTemplate.send(TOPIC, document.getCaseId().toString(), payload);
    }

    private static String json(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
