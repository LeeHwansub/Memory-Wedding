package com.memorywedding.project;

import com.memorywedding.common.BadRequestException;
import com.memorywedding.common.ForbiddenException;
import com.memorywedding.common.NotFoundException;
import com.memorywedding.domain.entity.Invitation;
import com.memorywedding.domain.entity.InviteLink;
import com.memorywedding.domain.entity.Member;
import com.memorywedding.domain.entity.WeddingProject;
import com.memorywedding.domain.enums.ProjectStatus;
import com.memorywedding.domain.repository.InvitationRepository;
import com.memorywedding.domain.repository.InviteLinkRepository;
import com.memorywedding.domain.repository.MemberRepository;
import com.memorywedding.domain.repository.WeddingProjectRepository;
import com.memorywedding.project.dto.CreateProjectRequest;
import com.memorywedding.project.dto.ProjectResponse;
import com.memorywedding.project.dto.UpdateProjectRequest;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeddingProjectService {

    private static final int MVP_MAX_PROJECTS_PER_MEMBER = 1;

    private final WeddingProjectRepository weddingProjectRepository;
    private final InvitationRepository invitationRepository;
    private final InviteLinkRepository inviteLinkRepository;
    private final MemberRepository memberRepository;

    public List<ProjectResponse> listMyProjects(Long memberId) {
        return weddingProjectRepository
                .findByOwner_IdAndDeletedAtIsNullOrderByCreatedAtDesc(memberId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ProjectResponse getProject(Long memberId, Long projectId) {
        WeddingProject project = getOwnedProject(memberId, projectId);
        return toResponse(project);
    }

    @Transactional
    public ProjectResponse createProject(Long memberId, CreateProjectRequest request) {
        Member owner = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new NotFoundException("Member not found"));

        long count = weddingProjectRepository.countByOwner_IdAndDeletedAtIsNull(memberId);
        if (count >= MVP_MAX_PROJECTS_PER_MEMBER) {
            throw new BadRequestException("MVP에서는 Wedding Project를 1개만 생성할 수 있습니다.");
        }

        String slug = generateUniqueSlug(request.groomName(), request.brideName());

        WeddingProject project = weddingProjectRepository.save(WeddingProject.builder()
                .owner(owner)
                .slug(slug)
                .groomName(request.groomName().trim())
                .brideName(request.brideName().trim())
                .weddingAt(request.weddingAt())
                .venueName(trimToNull(request.venueName()))
                .venueAddress(trimToNull(request.venueAddress()))
                .status(ProjectStatus.DRAFT)
                .build());

        invitationRepository.save(Invitation.builder()
                .project(project)
                .title(request.groomName().trim() + " ♥ " + request.brideName().trim())
                .greetingMessage("소중한 분을 초대합니다.")
                .build());

        inviteLinkRepository.save(InviteLink.builder()
                .project(project)
                .token(UUID.randomUUID().toString().replace("-", ""))
                .build());

        return toResponse(project);
    }

    @Transactional
    public ProjectResponse updateProject(Long memberId, Long projectId, UpdateProjectRequest request) {
        WeddingProject project = getOwnedProject(memberId, projectId);
        project.update(
                request.groomName().trim(),
                request.brideName().trim(),
                request.weddingAt(),
                trimToNull(request.venueName()),
                trimToNull(request.venueAddress()),
                request.status()
        );
        return toResponse(project);
    }

    @Transactional
    public void deleteProject(Long memberId, Long projectId) {
        WeddingProject project = getOwnedProject(memberId, projectId);
        project.softDelete();
        inviteLinkRepository.findByProject_Id(projectId).ifPresent(InviteLink::deactivate);
    }

    @Transactional
    public ProjectResponse toggleInviteLink(Long memberId, Long projectId, boolean active) {
        WeddingProject project = getOwnedProject(memberId, projectId);
        InviteLink inviteLink = inviteLinkRepository.findByProject_Id(projectId)
                .orElseThrow(() -> new NotFoundException("Invite link not found"));
        if (active) {
            inviteLink.activate();
        } else {
            inviteLink.deactivate();
        }
        return toResponse(project);
    }

    private WeddingProject getOwnedProject(Long memberId, Long projectId) {
        WeddingProject project = weddingProjectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new NotFoundException("Wedding Project not found"));
        if (!project.getOwner().getId().equals(memberId)) {
            throw new ForbiddenException("이 Wedding Project에 접근할 권한이 없습니다.");
        }
        return project;
    }

    private ProjectResponse toResponse(WeddingProject project) {
        InviteLink inviteLink = inviteLinkRepository.findByProject_Id(project.getId()).orElse(null);
        return ProjectResponse.from(project, inviteLink);
    }

    private String generateUniqueSlug(String groomName, String brideName) {
        String base = slugify(groomName) + "-" + slugify(brideName);
        if (base.equals("-") || base.length() < 3) {
            base = "wedding";
        }
        for (int i = 0; i < 10; i++) {
            String candidate = base + "-" + UUID.randomUUID().toString().substring(0, 8);
            if (!weddingProjectRepository.existsBySlug(candidate)) {
                return candidate;
            }
        }
        return "wedding-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String slugify(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9가-힣]+", "-")
                .replaceAll("(^-|-$)", "");
        if (normalized.isBlank()) {
            return "couple";
        }
        return normalized.length() > 20 ? normalized.substring(0, 20) : normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
