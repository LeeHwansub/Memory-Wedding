package com.memorywedding.domain.repository;

import com.memorywedding.domain.entity.WeddingProject;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeddingProjectRepository extends JpaRepository<WeddingProject, Long> {

    List<WeddingProject> findByOwner_IdAndDeletedAtIsNullOrderByCreatedAtDesc(Long ownerId);

    Optional<WeddingProject> findByIdAndDeletedAtIsNull(Long id);

    Optional<WeddingProject> findBySlugAndDeletedAtIsNull(String slug);

    long countByOwner_IdAndDeletedAtIsNull(Long ownerId);

    boolean existsBySlug(String slug);
}
