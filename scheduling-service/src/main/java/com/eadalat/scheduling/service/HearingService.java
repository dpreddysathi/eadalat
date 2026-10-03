package com.eadalat.scheduling.service;

import com.eadalat.scheduling.domain.Hearing;
import com.eadalat.scheduling.domain.HearingStatus;
import com.eadalat.scheduling.repository.HearingRepository;
import com.eadalat.scheduling.web.dto.RescheduleHearingRequest;
import com.eadalat.scheduling.web.dto.ScheduleHearingRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Core hearing lifecycle operations.
 */
@Service
public class HearingService {

    private final HearingRepository hearingRepository;
    private final HearingEventPublisher eventPublisher;

    public HearingService(HearingRepository hearingRepository, HearingEventPublisher eventPublisher) {
        this.hearingRepository = hearingRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Hearing schedule(ScheduleHearingRequest request, Long requesterUserId, boolean requesterIsAdmin) {
        Hearing hearing = new Hearing();
        hearing.setCaseId(request.getCaseId());
        hearing.setScheduledAt(request.getScheduledAt());
        hearing.setDurationMinutes(request.getDurationMinutes());
        hearing.setStatus(HearingStatus.SCHEDULED);

        Long judgeId = requesterUserId;
        if (requesterIsAdmin && request.getJudgeId() != null) {
            judgeId = request.getJudgeId();
        }
        hearing.setJudgeId(judgeId);

        Hearing saved = hearingRepository.save(hearing);
        eventPublisher.publish(saved, HearingEventPublisher.TYPE_SCHEDULED);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Hearing> list(Long caseId, boolean upcoming) {
        if (caseId != null && upcoming) {
            return hearingRepository.findByCaseIdAndStatusAndScheduledAtGreaterThanEqual(
                    caseId, HearingStatus.SCHEDULED, LocalDateTime.now());
        }
        if (caseId != null) {
            return hearingRepository.findByCaseId(caseId);
        }
        if (upcoming) {
            return hearingRepository.findByStatusAndScheduledAtGreaterThanEqual(
                    HearingStatus.SCHEDULED, LocalDateTime.now());
        }
        return hearingRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Hearing get(Long id) {
        return hearingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hearing not found: " + id));
    }

    @Transactional
    public Hearing reschedule(Long id, RescheduleHearingRequest request) {
        Hearing hearing = get(id);
        if (hearing.getStatus() == HearingStatus.CANCELLED || hearing.getStatus() == HearingStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot reschedule a hearing that is " + hearing.getStatus());
        }
        hearing.setScheduledAt(request.getScheduledAt());
        hearing.setDurationMinutes(request.getDurationMinutes());
        Hearing saved = hearingRepository.save(hearing);
        eventPublisher.publish(saved, HearingEventPublisher.TYPE_SCHEDULED);
        return saved;
    }

    @Transactional
    public Hearing cancel(Long id) {
        Hearing hearing = get(id);
        if (hearing.getStatus() == HearingStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot cancel a completed hearing");
        }
        hearing.setStatus(HearingStatus.CANCELLED);
        Hearing saved = hearingRepository.save(hearing);
        eventPublisher.publish(saved, HearingEventPublisher.TYPE_CANCELLED);
        return saved;
    }

    @Transactional
    public Hearing complete(Long id) {
        Hearing hearing = get(id);
        if (hearing.getStatus() == HearingStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot complete a cancelled hearing");
        }
        hearing.setStatus(HearingStatus.COMPLETED);
        Hearing saved = hearingRepository.save(hearing);
        eventPublisher.publish(saved, HearingEventPublisher.TYPE_COMPLETED);
        return saved;
    }
}
