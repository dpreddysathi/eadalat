package com.eadalat.casework.service;

import com.eadalat.casework.dto.AssignCaseRequest;
import com.eadalat.casework.dto.CaseSummaryResponse;
import com.eadalat.casework.dto.CreateCaseRequest;
import com.eadalat.casework.dto.UpdateStatusRequest;
import com.eadalat.casework.kafka.CaseEventProducer;
import com.eadalat.casework.model.Case;
import com.eadalat.casework.model.CaseStatus;
import com.eadalat.casework.repository.CaseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CaseService {

    private final CaseRepository caseRepository;
    private final CaseEventProducer eventProducer;
    private final SummaryUtil summaryUtil;

    public CaseService(CaseRepository caseRepository,
                       CaseEventProducer eventProducer,
                       SummaryUtil summaryUtil) {
        this.caseRepository = caseRepository;
        this.eventProducer = eventProducer;
        this.summaryUtil = summaryUtil;
    }

    @Transactional
    public Case fileCase(CreateCaseRequest request, Long petitionerId) {
        Case c = new Case();
        c.setTitle(request.getTitle());
        c.setDescription(request.getDescription());
        c.setCategory(request.getCategory());
        c.setStatus(CaseStatus.FILED);
        c.setPetitionerId(petitionerId);
        Case saved = caseRepository.save(c);
        eventProducer.publish(CaseEventProducer.EventType.CASE_FILED,
                saved.getId(), saved.getStatus().name(),
                saved.getPetitionerId(), null, null, petitionerId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Case> listCases(String role, Long userId, CaseStatus statusFilter) {
        return switch (role) {
            case "PETITIONER" -> statusFilter == null
                    ? caseRepository.findByPetitionerId(userId)
                    : caseRepository.findByPetitionerIdAndStatus(userId, statusFilter);
            case "LAWYER" -> statusFilter == null
                    ? caseRepository.findByLawyerId(userId)
                    : caseRepository.findByLawyerIdAndStatus(userId, statusFilter);
            case "JUDGE" -> statusFilter == null
                    ? caseRepository.findByJudgeId(userId)
                    : caseRepository.findByJudgeIdAndStatus(userId, statusFilter);
            case "ADMIN" -> statusFilter == null
                    ? caseRepository.findAll()
                    : caseRepository.findByStatus(statusFilter);
            default -> throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unsupported role");
        };
    }

    @Transactional(readOnly = true)
    public Case getCase(Long id, String role, Long userId) {
        Case c = findOrThrow(id);
        if ("ADMIN".equals(role) || isParticipant(c, userId)) {
            return c;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a participant of this case");
    }

    @Transactional
    public Case assignCase(Long id, AssignCaseRequest request, Long actorId) {
        Case c = findOrThrow(id);
        c.setJudgeId(request.getJudgeId());
        c.setLawyerId(request.getLawyerId());
        c.setStatus(CaseStatus.ASSIGNED);
        Case saved = caseRepository.save(c);
        eventProducer.publish(CaseEventProducer.EventType.CASE_ASSIGNED,
                saved.getId(), saved.getStatus().name(),
                saved.getPetitionerId(), saved.getLawyerId(), saved.getJudgeId(), actorId);
        return saved;
    }

    @Transactional
    public Case updateStatus(Long id, UpdateStatusRequest request, String role, Long userId) {
        Case c = findOrThrow(id);
        boolean allowed = "ADMIN".equals(role)
                || ("JUDGE".equals(role) && userId.equals(c.getJudgeId()));
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the assigned judge or an admin can change case status");
        }
        CaseStatus next = request.getStatus();
        if (c.getStatus() == next) {
            return c;
        }
        if (!c.getStatus().canTransitionTo(next)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Illegal status transition from " + c.getStatus() + " to " + next);
        }
        c.setStatus(next);
        Case saved = caseRepository.save(c);
        eventProducer.publish(CaseEventProducer.EventType.STATUS_CHANGED,
                saved.getId(), saved.getStatus().name(),
                saved.getPetitionerId(), saved.getLawyerId(), saved.getJudgeId(), userId);
        return saved;
    }

    @Transactional(readOnly = true)
    public CaseSummaryResponse buildSummary(Long id, String role, Long userId) {
        Case c = findOrThrow(id);
        boolean allowed = "ADMIN".equals(role)
                || "JUDGE".equals(role)
                || "LAWYER".equals(role)
                || userId.equals(c.getPetitionerId());
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to view this case summary");
        }
        return new CaseSummaryResponse(
                c.getId(),
                summaryUtil.buildSummary(c.getTitle(), c.getDescription()),
                summaryUtil.extractKeywords(c.getTitle(), c.getDescription()));
    }

    private Case findOrThrow(Long id) {
        return caseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found"));
    }

    private boolean isParticipant(Case c, Long userId) {
        return userId != null && (userId.equals(c.getPetitionerId())
                || userId.equals(c.getLawyerId())
                || userId.equals(c.getJudgeId()));
    }
}
