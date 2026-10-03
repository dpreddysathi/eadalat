package com.eadalat.scheduling.repository;

import com.eadalat.scheduling.domain.Hearing;
import com.eadalat.scheduling.domain.HearingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface HearingRepository extends JpaRepository<Hearing, Long> {

    List<Hearing> findByCaseId(Long caseId);

    List<Hearing> findByStatusAndScheduledAtGreaterThanEqual(HearingStatus status, LocalDateTime from);

    List<Hearing> findByCaseIdAndStatusAndScheduledAtGreaterThanEqual(Long caseId, HearingStatus status, LocalDateTime from);
}
