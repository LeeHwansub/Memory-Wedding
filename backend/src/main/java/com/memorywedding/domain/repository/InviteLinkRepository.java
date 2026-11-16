package com.memorywedding.domain.repository;

import com.memorywedding.domain.entity.InviteLink;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InviteLinkRepository extends JpaRepository<InviteLink, Long> {

    Optional<InviteLink> findByProject_Id(Long projectId);

    Optional<InviteLink> findByToken(String token);
}
