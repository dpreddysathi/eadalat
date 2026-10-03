package com.eadalat.scheduling.web;

import com.eadalat.scheduling.domain.Hearing;
import com.eadalat.scheduling.service.HearingService;
import com.eadalat.scheduling.web.dto.HearingResponse;
import com.eadalat.scheduling.web.dto.RescheduleHearingRequest;
import com.eadalat.scheduling.web.dto.ScheduleHearingRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Hearing scheduling endpoints.
 */
@RestController
@RequestMapping("/api/hearings")
public class HearingController {

    private final HearingService hearingService;

    public HearingController(HearingService hearingService) {
        this.hearingService = hearingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('JUDGE', 'ADMIN')")
    public HearingResponse schedule(
            @Valid @RequestBody ScheduleHearingRequest request,
            Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        Hearing hearing = hearingService.schedule(request, userId, isAdmin);
        return HearingResponse.from(hearing);
    }

    @GetMapping
    public List<HearingResponse> list(
            @RequestParam(required = false) Long caseId,
            @RequestParam(required = false, defaultValue = "false") boolean upcoming) {
        return hearingService.list(caseId, upcoming).stream()
                .map(HearingResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public HearingResponse get(@PathVariable Long id) {
        return HearingResponse.from(hearingService.get(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('JUDGE', 'ADMIN')")
    public HearingResponse reschedule(
            @PathVariable Long id,
            @Valid @RequestBody RescheduleHearingRequest request) {
        return HearingResponse.from(hearingService.reschedule(id, request));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('JUDGE', 'ADMIN')")
    public HearingResponse cancel(@PathVariable Long id) {
        return HearingResponse.from(hearingService.cancel(id));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('JUDGE', 'ADMIN')")
    public HearingResponse complete(@PathVariable Long id) {
        return HearingResponse.from(hearingService.complete(id));
    }
}
