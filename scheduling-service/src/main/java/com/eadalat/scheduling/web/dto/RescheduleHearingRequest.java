package com.eadalat.scheduling.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Request body for rescheduling an existing hearing.
 */
@Getter
@Setter
public class RescheduleHearingRequest {

    @NotNull(message = "scheduledAt is required")
    private LocalDateTime scheduledAt;

    @Min(value = 1, message = "durationMinutes must be positive")
    private int durationMinutes = 60;
}
