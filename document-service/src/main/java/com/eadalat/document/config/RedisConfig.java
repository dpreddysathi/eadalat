package com.eadalat.document.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class RedisConfig {
    // Connection details are provided by Spring Boot auto-configuration
    // via spring.data.redis.host / spring.data.redis.port.
}
