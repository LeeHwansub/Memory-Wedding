package com.memorywedding.invitation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorywedding.common.BadRequestException;
import com.memorywedding.common.ForbiddenException;
import com.memorywedding.common.NotFoundException;
import com.memorywedding.domain.entity.Invitation;
import com.memorywedding.domain.entity.InviteLink;
import com.memorywedding.domain.entity.WeddingProject;
import com.memorywedding.domain.repository.InvitationRepository;
import com.memorywedding.domain.repository.InviteLinkRepository;
import com.memorywedding.domain.repository.WeddingProjectRepository;
import com.memorywedding.invitation.dto.AccountEntry;
import com.memorywedding.invitation.dto.InvitationResponse;
import com.memorywedding.invitation.dto.PublicInvitationResponse;
import com.memorywedding.invitation.dto.UpdateInvitationRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final WeddingProjectRepository weddingProjectRepository;
    private final InviteLinkRepository inviteLinkRepository;
    private final ObjectMapper objectMapper;

    public InvitationResponse getForOwner(Long memberId, Long projectId) {
        Invitation invitation = getOwnedInvitation(memberId, projectId);
        return toResponse(invitation);
    }

    @Transactional
    public InvitationResponse updateForOwner(Long memberId, Long projectId, UpdateInvitationRequest request) {
        Invitation invitation = getOwnedInvitation(memberId, projectId);
        List<AccountEntry> accounts = request.accounts() == null ? List.of() : request.accounts();
        invitation.updateContent(
                trimToNull(request.title()),
                request.greetingMessage(),
                buildMapUrl(invitation.getProject().getVenueAddress()),
                writeAccounts(accounts)
        );
        return toResponse(invitation);
    }

    @Transactional
    public InvitationResponse setPublished(Long memberId, Long projectId, boolean published) {
        Invitation invitation = getOwnedInvitation(memberId, projectId);
        if (published) {
            InviteLink link = inviteLinkRepository.findByProject_Id(projectId)
                    .orElseThrow(() -> new NotFoundException("Invite link not found"));
            if (!link.isActive()) {
                throw new BadRequestException("초대 링크가 비활성 상태입니다. 먼저 링크를 활성화해 주세요.");
            }
        }
        invitation.setPublished(published);
        return toResponse(invitation);
    }

    public PublicInvitationResponse getPublicBySlug(String slug) {
        WeddingProject project = weddingProjectRepository.findBySlugAndDeletedAtIsNull(slug)
                .orElseThrow(() -> new NotFoundException("청첩장을 찾을 수 없습니다."));

        InviteLink link = inviteLinkRepository.findByProject_Id(project.getId())
                .orElseThrow(() -> new NotFoundException("청첩장을 찾을 수 없습니다."));
        if (!link.isActive()) {
            throw new ForbiddenException("이 청첩장 링크는 비활성화되어 있습니다.");
        }

        Invitation invitation = invitationRepository.findByProject_Id(project.getId())
                .orElseThrow(() -> new NotFoundException("청첩장을 찾을 수 없습니다."));
        if (!invitation.isPublished()) {
            throw new ForbiddenException("아직 공개되지 않은 청첩장입니다.");
        }

        return new PublicInvitationResponse(
                invitation.getTitle(),
                invitation.getGreetingMessage(),
                buildMapUrl(project.getVenueAddress()),
                readAccounts(invitation.getAccountInfo()),
                project.getGroomName(),
                project.getBrideName(),
                project.getWeddingAt(),
                project.getVenueName(),
                project.getVenueAddress(),
                project.getSlug()
        );
    }

    private InvitationResponse toResponse(Invitation invitation) {
        return InvitationResponse.from(
                invitation,
                readAccounts(invitation.getAccountInfo()),
                buildMapUrl(invitation.getProject().getVenueAddress())
        );
    }

    private Invitation getOwnedInvitation(Long memberId, Long projectId) {
        WeddingProject project = weddingProjectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new NotFoundException("Wedding Project not found"));
        if (!project.getOwner().getId().equals(memberId)) {
            throw new ForbiddenException("이 Wedding Project에 접근할 권한이 없습니다.");
        }
        return invitationRepository.findByProject_Id(projectId)
                .orElseThrow(() -> new NotFoundException("Invitation not found"));
    }

    private String buildMapUrl(String venueAddress) {
        if (venueAddress == null || venueAddress.isBlank()) {
            return null;
        }
        return "https://map.kakao.com/?q=" + URLEncoder.encode(venueAddress.trim(), StandardCharsets.UTF_8);
    }

    private List<AccountEntry> readAccounts(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    private String writeAccounts(List<AccountEntry> accounts) {
        try {
            return objectMapper.writeValueAsString(accounts);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("축의금 계좌 정보를 저장할 수 없습니다.");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
