package com.memorywedding.domain.repository;

import com.memorywedding.domain.entity.DriveConnection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriveConnectionRepository extends JpaRepository<DriveConnection, Long> {

    Optional<DriveConnection> findByMember_Id(Long memberId);

    boolean existsByMember_Id(Long memberId);

    void deleteByMember_Id(Long memberId);
}
