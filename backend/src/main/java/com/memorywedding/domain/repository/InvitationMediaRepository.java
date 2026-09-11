package com.memorywedding.domain.repository;

import com.memorywedding.domain.entity.InvitationMedia;
import com.memorywedding.domain.enums.InvitationMediaType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationMediaRepository extends JpaRepository<InvitationMedia, Long> {

    List<InvitationMedia> findByInvitation_IdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(Long invitationId);

    List<InvitationMedia> findByInvitation_IdAndMediaTypeAndDeletedAtIsNullOrderBySortOrderAscIdAsc(
            Long invitationId,
            InvitationMediaType mediaType);

    Optional<InvitationMedia> findByIdAndInvitation_IdAndDeletedAtIsNull(Long id, Long invitationId);

    long countByInvitation_IdAndMediaTypeAndDeletedAtIsNull(Long invitationId, InvitationMediaType mediaType);
}
