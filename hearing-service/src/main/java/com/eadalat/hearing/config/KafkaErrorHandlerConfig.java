package com.eadalat.hearing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Failed records (e.g. poison JSON) go straight to the dead-letter topic;
 * no retries on records that can never succeed.
 */
@Configuration
public class KafkaErrorHandlerConfig {

    @Bean
    public CommonErrorHandler kafkaListenerErrorHandler(KafkaTemplate<String, String> stringKafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(stringKafkaTemplate);
        return new DefaultErrorHandler(recoverer, new FixedBackOff(0L, 0L));
    }
}
