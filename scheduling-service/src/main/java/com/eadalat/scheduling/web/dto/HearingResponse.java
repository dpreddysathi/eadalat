package com.eadalat.scheduling.web.dto;

import com.eadalat.scheduling.domain.Hearing;
import com.eadalat.scheduling.domain.HearingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Read model for a hearing.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HearingResponse {

    private Long id;
    private Long caseId;
    private Long judgeId;
    private LocalDateTime scheduledAt;
    private int durationMinutes;
    private HearingStatus status;
    private String roomId;

    public static HearingResponse from(Hearing hearing) {
        return HearingResponse.builder()
                .id(hearing.getId())
                .caseId(hearing.getCaseId())
                .judgeId(hearing.getJudgeId())
                .scheduledAt(hearing.getScheduledAt())
                .durationMinutes(hearing.getDurationMinutes())
                .status(hearing.getStatus())
                .roomId(hearing.getRoomId())
                .build();
    }
}
