package com.memorywedding.domain.repository;

import com.memorywedding.domain.entity.AiPhotoResult;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiPhotoResultRepository extends JpaRepository<AiPhotoResult, Long> {

    List<AiPhotoResult> findByJob_IdOrderBySceneCategoryAscIdAsc(Long jobId);

    Optional<AiPhotoResult> findByUploadFile_Id(Long uploadFileId);

    void deleteByJob_Project_Id(Long projectId);
}
