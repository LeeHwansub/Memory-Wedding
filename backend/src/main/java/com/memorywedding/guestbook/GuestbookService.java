package com.memorywedding.guestbook;

import com.memorywedding.common.BadRequestException;
import com.memorywedding.common.ForbiddenException;
import com.memorywedding.common.NotFoundException;
import com.memorywedding.domain.entity.GuestbookEntry;
import com.memorywedding.domain.entity.GuestbookLike;
import com.memorywedding.domain.entity.Invitation;
import com.memorywedding.domain.entity.InviteLink;
import com.memorywedding.domain.entity.WeddingProject;
import com.memorywedding.domain.repository.GuestbookEntryRepository;
import com.memorywedding.domain.repository.GuestbookLikeRepository;
import com.memorywedding.domain.repository.InvitationRepository;
import com.memorywedding.domain.repository.InviteLinkRepository;
import com.memorywedding.domain.repository.WeddingProjectRepository;
import com.memorywedding.guestbook.dto.CreateGuestbookRequest;
import com.memorywedding.guestbook.dto.GuestbookEntryResponse;
import com.memorywedding.guestbook.dto.GuestbookPageResponse;
import com.memorywedding.guestbook.dto.UpdateGuestbookRequest;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuestbookService {

    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(5, 10, 20);
    private static final Set<String> BLOCKED_WORDS = Set.of(
            "시발", "씨발", "병신", "좆", "지랄", "fuck", "shit"
    );

    private final GuestbookEntryRepository guestbookEntryRepository;
    private final GuestbookLikeRepository guestbookLikeRepository;
    private final WeddingProjectRepository weddingProjectRepository;
    private final InviteLinkRepository inviteLinkRepository;
    private final InvitationRepository invitationRepository;

    public GuestbookPageResponse listPublic(String slug, int page, int size, String clientKey) {
        WeddingProject project = resolvePublicProject(slug);
        return toPage(project.getId(), page, size, clientKey);
    }

    @Transactional
    public GuestbookEntryResponse createPublic(String slug, CreateGuestbookRequest request) {
        WeddingProject project = resolvePublicProject(slug);
        String key = normalizeClientKey(request.clientKey());
        String guestName = request.guestName().trim();
        String message = request.message().trim();
        validateContent(guestName, message);

        if (guestbookEntryRepository.existsByProject_IdAndClientKeyAndDeletedAtIsNull(project.getId(), key)) {
            throw new BadRequestException("이미 작성한 방명록이 있습니다. 기존 글을 수정해 주세요.");
        }

        GuestbookEntry entry = guestbookEntryRepository.save(GuestbookEntry.builder()
                .project(project)
                .guestName(guestName)
                .message(message)
                .clientKey(key)
                .build());
        return GuestbookEntryResponse.from(entry, false, true);
    }

    @Transactional
    public GuestbookEntryResponse updatePublic(String slug, Long entryId, UpdateGuestbookRequest request) {
        WeddingProject project = resolvePublicProject(slug);
        String key = normalizeClientKey(request.clientKey());
        String guestName = request.guestName().trim();
        String message = request.message().trim();
        validateContent(guestName, message);

        GuestbookEntry entry = guestbookEntryRepository
                .findByIdAndProject_IdAndDeletedAtIsNull(entryId, project.getId())
                .orElseThrow(() -> new NotFoundException("방명록을 찾을 수 없습니다."));

        if (!key.equals(entry.getClientKey())) {
            throw new ForbiddenException("본인이 작성한 방명록만 수정할 수 있습니다.");
        }

        entry.updateContent(guestName, message);
        boolean liked = guestbookLikeRepository.existsByEntry_IdAndClientKey(entryId, key);
        return GuestbookEntryResponse.from(entry, liked, true);
    }

    @Transactional
    public GuestbookEntryResponse likePublic(String slug, Long entryId, String clientKey) {
        WeddingProject project = resolvePublicProject(slug);
        String key = normalizeClientKey(clientKey);
        GuestbookEntry entry = guestbookEntryRepository
                .findByIdAndProject_IdAndDeletedAtIsNull(entryId, project.getId())
                .orElseThrow(() -> new NotFoundException("방명록을 찾을 수 없습니다."));

        boolean mine = key.equals(entry.getClientKey());
        if (guestbookLikeRepository.existsByEntry_IdAndClientKey(entryId, key)) {
            return GuestbookEntryResponse.from(entry, true, mine);
        }

        guestbookLikeRepository.save(GuestbookLike.builder()
                .entry(entry)
                .clientKey(key)
                .build());
        entry.increaseLike();
        return GuestbookEntryResponse.from(entry, true, mine);
    }

    public GuestbookPageResponse listForOwner(Long memberId, Long projectId, int page, int size) {
        WeddingProject project = getOwnedProject(memberId, projectId);
        return toPage(project.getId(), page, size, null);
    }

    @Transactional
    public void deleteForOwner(Long memberId, Long projectId, Long entryId) {
        getOwnedProject(memberId, projectId);
        GuestbookEntry entry = guestbookEntryRepository
                .findByIdAndProject_IdAndDeletedAtIsNull(entryId, projectId)
                .orElseThrow(() -> new NotFoundException("방명록을 찾을 수 없습니다."));
        entry.softDelete();
    }

    private GuestbookPageResponse toPage(Long projectId, int page, int size, String clientKey) {
        int safePage = Math.max(page, 0);
        int safeSize = ALLOWED_PAGE_SIZES.contains(size) ? size : 10;
        Page<GuestbookEntry> result = guestbookEntryRepository
                .findByProject_IdAndDeletedAtIsNullOrderByCreatedAtDesc(
                        projectId,
                        PageRequest.of(safePage, safeSize)
                );

        String key = clientKey == null || clientKey.isBlank() ? null : normalizeClientKey(clientKey);
        List<GuestbookEntryResponse> content = result.getContent().stream()
                .map(entry -> toResponse(entry, key))
                .toList();

        GuestbookEntryResponse myEntry = null;
        if (key != null) {
            myEntry = guestbookEntryRepository
                    .findByProject_IdAndClientKeyAndDeletedAtIsNull(projectId, key)
                    .map(entry -> toResponse(entry, key))
                    .orElse(null);
        }

        return new GuestbookPageResponse(
                content,
                myEntry,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private GuestbookEntryResponse toResponse(GuestbookEntry entry, String clientKey) {
        boolean liked = clientKey != null
                && guestbookLikeRepository.existsByEntry_IdAndClientKey(entry.getId(), clientKey);
        boolean mine = clientKey != null && clientKey.equals(entry.getClientKey());
        return GuestbookEntryResponse.from(entry, liked, mine);
    }

    private WeddingProject resolvePublicProject(String slug) {
        WeddingProject project = weddingProjectRepository.findBySlugAndDeletedAtIsNull(slug)
                .orElseThrow(() -> new NotFoundException("방명록을 열 수 없습니다."));

        InviteLink link = inviteLinkRepository.findByProject_Id(project.getId())
                .orElseThrow(() -> new NotFoundException("방명록을 열 수 없습니다."));
        if (!link.isActive()) {
            throw new ForbiddenException("이 초대 링크는 비활성화되어 있습니다.");
        }

        Invitation invitation = invitationRepository.findByProject_Id(project.getId())
                .orElseThrow(() -> new NotFoundException("방명록을 열 수 없습니다."));
        if (!invitation.isPublished()) {
            throw new ForbiddenException("아직 공개되지 않은 청첩장입니다.");
        }

        return project;
    }

    private WeddingProject getOwnedProject(Long memberId, Long projectId) {
        WeddingProject project = weddingProjectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new NotFoundException("Wedding Project not found"));
        if (!project.getOwner().getId().equals(memberId)) {
            throw new ForbiddenException("이 Wedding Project에 접근할 권한이 없습니다.");
        }
        return project;
    }

    private void validateContent(String guestName, String message) {
        if (guestName.isBlank() || message.isBlank()) {
            throw new BadRequestException("이름과 메시지를 모두 입력해 주세요.");
        }
        String combined = (guestName + " " + message).toLowerCase(Locale.ROOT);
        for (String word : BLOCKED_WORDS) {
            if (combined.contains(word.toLowerCase(Locale.ROOT))) {
                throw new BadRequestException("부적절한 표현이 포함되어 있습니다. 내용을 수정해 주세요.");
            }
        }
    }

    private String normalizeClientKey(String clientKey) {
        String key = clientKey == null ? "" : clientKey.trim();
        if (key.isBlank() || key.length() > 64) {
            throw new BadRequestException("작성자 식별자가 올바르지 않습니다.");
        }
        return key;
    }
}
