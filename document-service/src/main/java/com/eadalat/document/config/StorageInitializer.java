package com.eadalat.document.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Component
@RequiredArgsConstructor
public class StorageInitializer {

    private final DocumentProperties documentProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureUploadDir() throws IOException {
        Files.createDirectories(Paths.get(documentProperties.getUploadDir()));
    }
}
