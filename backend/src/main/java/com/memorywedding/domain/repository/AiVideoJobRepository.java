package com.memorywedding.domain.repository;

import com.memorywedding.domain.entity.AiVideoJob;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiVideoJobRepository extends JpaRepository<AiVideoJob, Long> {

    Optional<AiVideoJob> findFirstByProject_IdOrderByCreatedAtDesc(Long projectId);
}
