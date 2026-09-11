package com.memorywedding.domain.repository;

import com.memorywedding.domain.entity.UploadFile;
import com.memorywedding.domain.enums.FileType;
import com.memorywedding.domain.enums.UploadStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UploadFileRepository extends JpaRepository<UploadFile, Long> {

    Page<UploadFile> findByProject_IdAndDeletedAtIsNullOrderByCreatedAtDesc(
            Long projectId,
            Pageable pageable);

    Page<UploadFile> findByProject_IdAndUploadStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
            Long projectId,
            UploadStatus uploadStatus,
            Pageable pageable);

    Page<UploadFile> findByProject_IdAndFileTypeAndGuestNameAndDeletedAtIsNullOrderByCreatedAtDesc(
            Long projectId,
            FileType fileType,
            String guestName,
            Pageable pageable);

    long countByProject_IdAndFileTypeAndDeletedAtIsNull(Long projectId, FileType fileType);

    @Query("""
            SELECT u.guestName, COUNT(u)
            FROM UploadFile u
            WHERE u.project.id = :projectId
              AND u.deletedAt IS NULL
              AND u.fileType = :fileType
            GROUP BY u.guestName
            ORDER BY u.guestName ASC
            """)
    List<Object[]> countByGuestName(
            @Param("projectId") Long projectId,
            @Param("fileType") FileType fileType);

    Optional<UploadFile> findByIdAndProject_IdAndDeletedAtIsNull(Long id, Long projectId);

    long countByProject_IdAndUploadStatusAndDriveFileIdIsNullAndDeletedAtIsNull(
            Long projectId,
            UploadStatus uploadStatus);

    long countByProject_IdAndUploadStatusAndDriveFileIdIsNotNullAndDeletedAtIsNull(
            Long projectId,
            UploadStatus uploadStatus);

    java.util.List<UploadFile> findByProject_IdAndUploadStatusAndDriveFileIdIsNullAndDeletedAtIsNullOrderByCreatedAtAsc(
            Long projectId,
            UploadStatus uploadStatus);
}
