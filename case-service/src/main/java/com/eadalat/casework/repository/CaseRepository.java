package com.eadalat.casework.repository;

import com.eadalat.casework.model.Case;
import com.eadalat.casework.model.CaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CaseRepository extends JpaRepository<Case, Long> {

    List<Case> findByPetitionerId(Long petitionerId);

    List<Case> findByPetitionerIdAndStatus(Long petitionerId, CaseStatus status);

    List<Case> findByLawyerId(Long lawyerId);

    List<Case> findByLawyerIdAndStatus(Long lawyerId, CaseStatus status);

    List<Case> findByJudgeId(Long judgeId);

    List<Case> findByJudgeIdAndStatus(Long judgeId, CaseStatus status);

    List<Case> findByStatus(CaseStatus status);
}
