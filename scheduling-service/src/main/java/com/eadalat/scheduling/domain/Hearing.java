package com.eadalat.scheduling.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A hearing scheduled for a dispute case.
 */
@Entity
@Table(name = "hearings")
@Getter
@Setter
@NoArgsConstructor
public class Hearing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long caseId;

    @Column(nullable = false)
    private Long judgeId;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    @Column(nullable = false)
    private int durationMinutes = 60;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HearingStatus status = HearingStatus.SCHEDULED;

    @Column(length = 64)
    private String roomId;

    @PrePersist
    protected void prePersist() {
        if (roomId == null || roomId.isBlank()) {
            roomId = "room-" + caseId + "-" + UUID.randomUUID().toString().substring(0, 8);
        }
    }
}
