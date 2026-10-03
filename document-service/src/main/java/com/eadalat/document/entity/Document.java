package com.eadalat.document.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "documents", indexes = {
        @Index(name = "idx_documents_case", columnList = "caseId"),
        @Index(name = "idx_documents_case_filename", columnList = "caseId,fileName")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long caseId;

    @Column(nullable = false, length = 512)
    private String fileName;

    @Column(nullable = false, length = 512)
    private String storedFileName;

    @Column(length = 128)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false)
    private Long uploadedBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
