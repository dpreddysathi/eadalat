package com.eadalat.casework.controller;

import com.eadalat.casework.dto.AssignCaseRequest;
import com.eadalat.casework.dto.CaseSummaryResponse;
import com.eadalat.casework.dto.CreateCaseRequest;
import com.eadalat.casework.dto.UpdateStatusRequest;
import com.eadalat.casework.model.Case;
import com.eadalat.casework.model.CaseStatus;
import com.eadalat.casework.security.SecurityHelper;
import com.eadalat.casework.service.CaseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/cases")
public class CaseController {

    private final CaseService caseService;
    private final SecurityHelper securityHelper;

    public CaseController(CaseService caseService, SecurityHelper securityHelper) {
        this.caseService = caseService;
        this.securityHelper = securityHelper;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PETITIONER', 'LAWYER', 'ADMIN')")
    public ResponseEntity<Case> fileCase(@Valid @RequestBody CreateCaseRequest request) {
        Long petitionerId = requireUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(caseService.fileCase(request, petitionerId));
    }

    @GetMapping
    public ResponseEntity<List<Case>> listCases(@RequestParam(required = false) CaseStatus status) {
        return ResponseEntity.ok(caseService.listCases(
                requireRole(), requireUserId(), status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Case> getCase(@PathVariable Long id) {
        return ResponseEntity.ok(caseService.getCase(id, requireRole(), requireUserId()));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Case> assignCase(@PathVariable Long id,
                                          @RequestBody AssignCaseRequest request) {
        return ResponseEntity.ok(caseService.assignCase(id, request, requireUserId()));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Case> updateStatus(@PathVariable Long id,
                                             @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(caseService.updateStatus(id, request, requireRole(), requireUserId()));
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<CaseSummaryResponse> getSummary(@PathVariable Long id) {
        return ResponseEntity.ok(caseService.buildSummary(id, requireRole(), requireUserId()));
    }

    private Long requireUserId() {
        Long userId = securityHelper.currentUserIdAsLong();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing user identity");
        }
        return userId;
    }

    private String requireRole() {
        String role = securityHelper.currentRole();
        if (role == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing role");
        }
        return role;
    }
}
