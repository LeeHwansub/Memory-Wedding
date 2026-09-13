package com.memorywedding.invitation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorywedding.common.BadRequestException;
import com.memorywedding.common.ForbiddenException;
import com.memorywedding.common.NotFoundException;
import com.memorywedding.domain.entity.Invitation;
import com.memorywedding.domain.entity.InvitationMedia;
import com.memorywedding.domain.entity.InviteLink;
import com.memorywedding.domain.entity.WeddingProject;
import com.memorywedding.domain.enums.GalleryLayout;
import com.memorywedding.domain.enums.InvitationMediaType;
import com.memorywedding.domain.enums.InvitationTemplate;
import com.memorywedding.domain.enums.MainPhotoPlacement;
import com.memorywedding.domain.enums.MediaDisplaySize;
import com.memorywedding.domain.repository.InvitationMediaRepository;
import com.memorywedding.domain.repository.InvitationRepository;
import com.memorywedding.domain.repository.InviteLinkRepository;
import com.memorywedding.domain.repository.WeddingProjectRepository;
import com.memorywedding.invitation.dto.AccountEntry;
import com.memorywedding.invitation.dto.InvitationMediaResponse;
import com.memorywedding.invitation.dto.InvitationResponse;
import com.memorywedding.invitation.dto.PublicInvitationResponse;
import com.memorywedding.invitation.dto.ReorderGalleryRequest;
import com.memorywedding.invitation.dto.UpdateInvitationRequest;
import com.memorywedding.storage.ObjectStorage;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvitationService {

    private static final int MAX_GALLERY = 15;
    private static final long MAX_PHOTO_BYTES = 10L * 1024 * 1024;
    private static final Set<String> PHOTO_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    private final InvitationRepository invitationRepository;
    private final InvitationMediaRepository invitationMediaRepository;
    private final WeddingProjectRepository weddingProjectRepository;
    private final InviteLinkRepository inviteLinkRepository;
    private final ObjectStorage objectStorage;
    private final ObjectMapper objectMapper;

    public InvitationResponse getForOwner(Long memberId, Long projectId) {
        Invitation invitation = getOwnedInvitation(memberId, projectId);
        return toOwnerResponse(invitation);
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
        invitation.updateMediaSettings(
                request.template(),
                request.galleryLayout(),
                request.galleryColumns(),
                request.galleryImageSize(),
                request.mainPhotoSize(),
                request.mainPhotoPlacement(),
                request.mainBrightness(),
                request.mainSaturation(),
                request.mainFocalX(),
                request.mainFocalY()
        );
        return toOwnerResponse(invitation);
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
        return toOwnerResponse(invitation);
    }

    @Transactional
    public InvitationMediaResponse uploadMedia(
            Long memberId,
            Long projectId,
            InvitationMediaType mediaType,
            MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("사진을 선택해 주세요.");
        }
        Invitation invitation = getOwnedInvitation(memberId, projectId);
        String mime = normalizeMime(file.getContentType(), file.getOriginalFilename());
        if (!PHOTO_TYPES.contains(mime)) {
            throw new BadRequestException("지원하지 않는 형식입니다. (JPG/PNG/WEBP)");
        }
        if (file.getSize() > MAX_PHOTO_BYTES) {
            throw new BadRequestException("사진은 10MB 이하만 업로드할 수 있습니다.");
        }

        if (mediaType == InvitationMediaType.MAIN) {
            invitationMediaRepository
                    .findByInvitation_IdAndMediaTypeAndDeletedAtIsNullOrderBySortOrderAscIdAsc(
                            invitation.getId(), InvitationMediaType.MAIN)
                    .forEach(existing -> {
                        if (existing.getStorageKey() != null) {
                            objectStorage.delete(existing.getStorageKey());
                        }
                        existing.softDelete();
                    });
        } else {
            long count = invitationMediaRepository.countByInvitation_IdAndMediaTypeAndDeletedAtIsNull(
                    invitation.getId(), InvitationMediaType.GALLERY);
            if (count >= MAX_GALLERY) {
                throw new BadRequestException("웨딩 갤러리는 최대 " + MAX_GALLERY + "장까지 등록할 수 있습니다.");
            }
        }

        String filename = safeFilename(file.getOriginalFilename());
        int sortOrder = mediaType == InvitationMediaType.MAIN
                ? 0
                : (int) invitationMediaRepository.countByInvitation_IdAndMediaTypeAndDeletedAtIsNull(
                        invitation.getId(), InvitationMediaType.GALLERY);

        InvitationMedia media = invitationMediaRepository.save(InvitationMedia.builder()
                .invitation(invitation)
                .mediaType(mediaType)
                .originalFilename(filename)
                .mimeType(mime)
                .fileSize(file.getSize())
                .sortOrder(sortOrder)
                .build());

        String objectKey = invitation.getProject().getSlug()
                + "/invitation/"
                + mediaType.name().toLowerCase(Locale.ROOT)
                + "/"
                + UUID.randomUUID().toString().replace("-", "")
                + "_"
                + filename;
        try (InputStream in = file.getInputStream()) {
            var stored = objectStorage.store(objectKey, in, file.getSize(), mime);
            media.markStored(stored.provider(), stored.objectKey());
        } catch (Exception e) {
            throw new BadRequestException("사진 업로드에 실패했습니다.");
        }

        return toMediaResponse(media, projectId, false);
    }

    @Transactional
    public void deleteMedia(Long memberId, Long projectId, Long mediaId) {
        Invitation invitation = getOwnedInvitation(memberId, projectId);
        InvitationMedia media = invitationMediaRepository
                .findByIdAndInvitation_IdAndDeletedAtIsNull(mediaId, invitation.getId())
                .orElseThrow(() -> new NotFoundException("사진을 찾을 수 없습니다."));
        if (media.getStorageKey() != null) {
            objectStorage.delete(media.getStorageKey());
        }
        media.softDelete();
    }

    @Transactional
    public InvitationResponse reorderGallery(Long memberId, Long projectId, ReorderGalleryRequest request) {
        Invitation invitation = getOwnedInvitation(memberId, projectId);
        List<Long> ids = request.galleryIds() == null ? List.of() : request.galleryIds();
        List<InvitationMedia> gallery = invitationMediaRepository
                .findByInvitation_IdAndMediaTypeAndDeletedAtIsNullOrderBySortOrderAscIdAsc(
                        invitation.getId(), InvitationMediaType.GALLERY);
        if (ids.size() != gallery.size() || !new HashSet<>(ids).containsAll(
                gallery.stream().map(InvitationMedia::getId).toList())) {
            throw new BadRequestException("갤러리 순서가 올바르지 않습니다.");
        }
        for (int i = 0; i < ids.size(); i++) {
            Long id = ids.get(i);
            InvitationMedia media = gallery.stream()
                    .filter(item -> item.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("갤러리 순서가 올바르지 않습니다."));
            media.updateSortOrder(i);
        }
        return toOwnerResponse(invitation);
    }

    public ResponseEntity<InputStreamResource> openMediaForOwner(Long memberId, Long projectId, Long mediaId) {
        Invitation invitation = getOwnedInvitation(memberId, projectId);
        return openMedia(invitation, mediaId);
    }

    public ResponseEntity<InputStreamResource> openMediaPublic(String slug, Long mediaId) {
        Invitation invitation = resolvePublishedInvitation(slug);
        return openMedia(invitation, mediaId);
    }

    public PublicInvitationResponse getPublicBySlug(String slug) {
        Invitation invitation = resolvePublishedInvitation(slug);
        WeddingProject project = invitation.getProject();
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
                project.getSlug(),
                invitation.getTemplate() == null
                        ? InvitationTemplate.CLASSIC
                        : invitation.getTemplate(),
                safeLayout(invitation),
                safeColumns(invitation),
                safeGallerySize(invitation),
                safeMainSize(invitation),
                safePlacement(invitation),
                safeBrightness(invitation),
                safeSaturation(invitation),
                invitation.getMainFocalX(),
                invitation.getMainFocalY(),
                findMain(invitation.getId(), null, true, project.getSlug()),
                findGallery(invitation.getId(), null, true, project.getSlug())
        );
    }

    private ResponseEntity<InputStreamResource> openMedia(Invitation invitation, Long mediaId) {
        InvitationMedia media = invitationMediaRepository
                .findByIdAndInvitation_IdAndDeletedAtIsNull(mediaId, invitation.getId())
                .orElseThrow(() -> new NotFoundException("사진을 찾을 수 없습니다."));
        if (media.getStorageKey() == null) {
            throw new BadRequestException("아직 열 수 없는 사진입니다.");
        }
        InputStream stream = objectStorage.open(media.getStorageKey());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(media.getMimeType()))
                .header("Cache-Control", "public, max-age=3600")
                .body(new InputStreamResource(stream));
    }

    private Invitation resolvePublishedInvitation(String slug) {
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
        return invitation;
    }

    private InvitationResponse toOwnerResponse(Invitation invitation) {
        Long projectId = invitation.getProject().getId();
        return InvitationResponse.from(
                invitation,
                readAccounts(invitation.getAccountInfo()),
                buildMapUrl(invitation.getProject().getVenueAddress()),
                findMain(invitation.getId(), projectId, false, null),
                findGallery(invitation.getId(), projectId, false, null)
        );
    }

    private InvitationMediaResponse findMain(
            Long invitationId,
            Long projectId,
            boolean publicPath,
            String slug) {
        return invitationMediaRepository
                .findByInvitation_IdAndMediaTypeAndDeletedAtIsNullOrderBySortOrderAscIdAsc(
                        invitationId, InvitationMediaType.MAIN)
                .stream()
                .findFirst()
                .map(media -> toMediaResponse(media, projectId, publicPath, slug))
                .orElse(null);
    }

    private List<InvitationMediaResponse> findGallery(
            Long invitationId,
            Long projectId,
            boolean publicPath,
            String slug) {
        return invitationMediaRepository
                .findByInvitation_IdAndMediaTypeAndDeletedAtIsNullOrderBySortOrderAscIdAsc(
                        invitationId, InvitationMediaType.GALLERY)
                .stream()
                .map(media -> toMediaResponse(media, projectId, publicPath, slug))
                .toList();
    }

    private InvitationMediaResponse toMediaResponse(InvitationMedia media, Long projectId, boolean publicPath) {
        return toMediaResponse(media, projectId, publicPath, null);
    }

    private InvitationMediaResponse toMediaResponse(
            InvitationMedia media,
            Long projectId,
            boolean publicPath,
            String slug) {
        String path = publicPath
                ? "/api/public/w/" + slug + "/media/" + media.getId() + "/content"
                : "/api/projects/" + projectId + "/invitation/media/" + media.getId() + "/content";
        return new InvitationMediaResponse(
                media.getId(),
                media.getMediaType().name(),
                media.getOriginalFilename(),
                media.getMimeType(),
                media.getFileSize(),
                media.getSortOrder(),
                path
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

    private GalleryLayout safeLayout(Invitation invitation) {
        return invitation.getGalleryLayout() == null ? GalleryLayout.SLIDER : invitation.getGalleryLayout();
    }

    private int safeColumns(Invitation invitation) {
        return invitation.getGalleryColumns() <= 0 ? 2 : invitation.getGalleryColumns();
    }

    private MediaDisplaySize safeGallerySize(Invitation invitation) {
        return invitation.getGalleryImageSize() == null ? MediaDisplaySize.MD : invitation.getGalleryImageSize();
    }

    private MediaDisplaySize safeMainSize(Invitation invitation) {
        return invitation.getMainPhotoSize() == null ? MediaDisplaySize.LG : invitation.getMainPhotoSize();
    }

    private MainPhotoPlacement safePlacement(Invitation invitation) {
        return invitation.getMainPhotoPlacement() == null
                ? MainPhotoPlacement.TOP
                : invitation.getMainPhotoPlacement();
    }

    private double safeBrightness(Invitation invitation) {
        return invitation.getMainBrightness() <= 0 ? 1.0 : invitation.getMainBrightness();
    }

    private double safeSaturation(Invitation invitation) {
        return invitation.getMainSaturation() <= 0 ? 1.0 : invitation.getMainSaturation();
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

    private String normalizeMime(String contentType, String filename) {
        if (contentType != null && !contentType.isBlank() && !"application/octet-stream".equals(contentType)) {
            return contentType.toLowerCase(Locale.ROOT);
        }
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        throw new BadRequestException("파일 형식을 확인할 수 없습니다.");
    }

    private String safeFilename(String original) {
        if (original == null || original.isBlank()) {
            return "photo.jpg";
        }
        String cleaned = original.replace("\\", "_").replace("/", "_").trim();
        return cleaned.length() > 200 ? cleaned.substring(cleaned.length() - 200) : cleaned;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
