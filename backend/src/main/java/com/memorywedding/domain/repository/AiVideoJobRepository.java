package com.memorywedding.domain.repository;

import com.memorywedding.domain.entity.AiVideoJob;
import com.memorywedding.domain.enums.AiJobStatus;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiVideoJobRepository extends JpaRepository<AiVideoJob, Long> {

    Optional<AiVideoJob> findFirstByProject_IdOrderByCreatedAtDesc(Long projectId);

    boolean existsByProject_IdAndStatusIn(Long projectId, Collection<AiJobStatus> statuses);
}
