package com.memorywedding.domain.repository;

import com.memorywedding.domain.entity.Invitation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    Optional<Invitation> findByProject_Id(Long projectId);
}
