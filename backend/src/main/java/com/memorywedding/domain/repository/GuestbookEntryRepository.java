package com.memorywedding.domain.repository;

import com.memorywedding.domain.entity.GuestbookEntry;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestbookEntryRepository extends JpaRepository<GuestbookEntry, Long> {

    Page<GuestbookEntry> findByProject_IdAndDeletedAtIsNullOrderByCreatedAtDesc(
            Long projectId,
            Pageable pageable);

    Optional<GuestbookEntry> findByIdAndProject_IdAndDeletedAtIsNull(Long id, Long projectId);

    Optional<GuestbookEntry> findByProject_IdAndClientKeyAndDeletedAtIsNull(
            Long projectId,
            String clientKey);

    boolean existsByProject_IdAndClientKeyAndDeletedAtIsNull(Long projectId, String clientKey);
}
