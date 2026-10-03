package com.eadalat.document.controller;

import com.eadalat.document.entity.Document;
import com.eadalat.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Document> upload(@RequestParam("file") MultipartFile file,
                                           @RequestParam("caseId") Long caseId,
                                           Authentication authentication) {
        Long uploadedBy = parseUserId(authentication);
        Document saved = documentService.uploadDocument(file, caseId, uploadedBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public List<Document> listByCase(@RequestParam("caseId") Long caseId) {
        return documentService.listByCase(caseId);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        DocumentService.Download download = documentService.download(id);
        Document document = download.document();
        MediaType contentType = MediaTypeFactory.getMediaType(document.getStoredFileName())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + document.getFileName()
                                .replace("\"", "") + "\"")
                .contentLength(document.getSizeBytes())
                .body(download.resource());
    }

    @GetMapping("/search")
    public List<Document> search(@RequestParam("q") String query) {
        return documentService.searchByFileName(query);
    }

    @GetMapping("/{id}/versions")
    public List<Document> versions(@PathVariable Long id) {
        return documentService.versions(id);
    }

    private static Long parseUserId(Authentication authentication) {
        try {
            return Long.parseLong(String.valueOf(authentication.getPrincipal()));
        } catch (NumberFormatException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "JWT subject is not a numeric user id");
        }
    }
}
