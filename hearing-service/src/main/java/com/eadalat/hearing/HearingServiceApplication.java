package com.eadalat.hearing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class HearingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(HearingServiceApplication.class, args);
    }
}
