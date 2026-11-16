package com.memorywedding.domain.repository;

import com.memorywedding.domain.entity.AiAnalysisJob;
import com.memorywedding.domain.enums.AiJobStatus;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiAnalysisJobRepository extends JpaRepository<AiAnalysisJob, Long> {

    Optional<AiAnalysisJob> findFirstByProject_IdOrderByCreatedAtDesc(Long projectId);

    Optional<AiAnalysisJob> findByIdAndProject_Id(Long id, Long projectId);

    boolean existsByProject_IdAndStatusIn(Long projectId, Collection<AiJobStatus> statuses);
}
