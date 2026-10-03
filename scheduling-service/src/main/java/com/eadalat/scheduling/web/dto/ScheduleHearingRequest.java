package com.eadalat.scheduling.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Request body for scheduling a new hearing.
 * ADMIN callers may optionally pass judgeId; otherwise it comes from the JWT subject.
 */
@Getter
@Setter
public class ScheduleHearingRequest {

    @NotNull(message = "caseId is required")
    private Long caseId;

    @NotNull(message = "scheduledAt is required")
    private LocalDateTime scheduledAt;

    @Min(value = 1, message = "durationMinutes must be positive")
    private int durationMinutes = 60;

    /** Optional override; only honored for ADMIN callers. */
    private Long judgeId;
}
