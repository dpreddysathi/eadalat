package com.eadalat.document.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class DocumentProperties {

    /**
     * Base directory on disk where uploaded files are stored.
     * Bound from {@code app.upload-dir}.
     */
    private String uploadDir = "/data/uploads";
}
