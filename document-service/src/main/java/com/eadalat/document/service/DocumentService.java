package com.eadalat.document.service;

import com.eadalat.document.config.DocumentProperties;
import com.eadalat.document.entity.Document;
import com.eadalat.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentProperties documentProperties;
    private final DocumentEventProducer eventProducer;

    @Transactional
    @CacheEvict(value = "caseDocs", key = "#caseId")
    public Document uploadDocument(MultipartFile file, Long caseId, Long uploadedBy) {
        String fileName = sanitize(file.getOriginalFilename());
        if (fileName == null || fileName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must have a name");
        }
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must not be empty");
        }

        List<Document> existing =
                documentRepository.findByCaseIdAndFileNameOrderByVersionDesc(caseId, fileName);
        int version = existing.isEmpty() ? 1 : existing.get(0).getVersion() + 1;

        String extension = extensionOf(fileName);
        String storedFileName = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
        Path target = Paths.get(documentProperties.getUploadDir()).resolve(storedFileName);
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to store file", e);
        }

        Document document = Document.builder()
                .caseId(caseId)
                .fileName(fileName)
                .storedFileName(storedFileName)
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .version(version)
                .uploadedBy(uploadedBy)
                .createdAt(Instant.now())
                .build();
        Document saved = documentRepository.save(document);
        eventProducer.documentUploaded(saved);
        return saved;
    }

    @Cacheable(value = "caseDocs", key = "#caseId")
    @Transactional(readOnly = true)
    public List<Document> listByCase(Long caseId) {
        return documentRepository.findByCaseId(caseId);
    }

    @Transactional(readOnly = true)
    public List<Document> searchByFileName(String query) {
        return documentRepository.findByFileNameContaining(query);
    }

    @Transactional(readOnly = true)
    public List<Document> versions(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Document not found: " + id));
        return documentRepository.findByCaseIdAndFileNameOrderByVersionDesc(
                document.getCaseId(), document.getFileName());
    }

    @Transactional(readOnly = true)
    public Download download(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Document not found: " + id));
        Path path = Paths.get(documentProperties.getUploadDir())
                .resolve(document.getStoredFileName());
        if (!Files.isRegularFile(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Stored file missing for document: " + id);
        }
        return new Download(new FileSystemResource(path), document);
    }

    private static String sanitize(String originalFilename) {
        if (originalFilename == null) {
            return null;
        }
        String name = Paths.get(originalFilename).getFileName().toString().trim();
        return name.isBlank() ? null : name;
    }

    private static String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return (dot > 0 && dot < fileName.length() - 1)
                ? fileName.substring(dot + 1).toLowerCase()
                : "";
    }

    public record Download(Resource resource, Document document) {
    }
}
