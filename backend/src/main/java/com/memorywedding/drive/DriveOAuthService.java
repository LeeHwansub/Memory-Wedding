package com.memorywedding.drive;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorywedding.auth.JwtTokenProvider;
import com.memorywedding.common.BadRequestException;
import com.memorywedding.common.NotFoundException;
import com.memorywedding.domain.entity.DriveConnection;
import com.memorywedding.domain.entity.Member;
import com.memorywedding.domain.repository.DriveConnectionRepository;
import com.memorywedding.domain.repository.MemberRepository;
import com.memorywedding.drive.dto.DriveConnectUrlResponse;
import com.memorywedding.drive.dto.DriveStatusResponse;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DriveOAuthService {

    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final String DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.file";

    private final JwtTokenProvider jwtTokenProvider;
    private final DriveConnectionRepository driveConnectionRepository;
    private final MemberRepository memberRepository;
    private final TokenEncryptor tokenEncryptor;
    private final GoogleDriveClient googleDriveClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${app.drive.redirect-uri}")
    private String redirectUri;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public DriveStatusResponse status(Long memberId) {
        return driveConnectionRepository.findByMember_Id(memberId)
                .map(c -> new DriveStatusResponse(
                        true,
                        c.getGoogleAccountEmail(),
                        c.getDriveRootFolderId() != null && !c.getDriveRootFolderId().isBlank()))
                .orElseGet(() -> new DriveStatusResponse(false, null, false));
    }

    public DriveConnectUrlResponse createAuthorizationUrl(Long memberId, Long projectId) {
        String state = jwtTokenProvider.createDriveOAuthState(memberId, projectId);
        String url = UriComponentsBuilder.fromUriString(AUTH_URL)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", DRIVE_SCOPE + " openid email profile")
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .queryParam("state", state)
                .encode()
                .toUriString();
        return new DriveConnectUrlResponse(url);
    }

    @Transactional
    public String handleCallback(String code, String state) {
        Long memberId;
        Long projectId = null;
        try {
            memberId = jwtTokenProvider.parseDriveOAuthState(state);
            projectId = jwtTokenProvider.parseDriveOAuthProjectId(state);
        } catch (Exception e) {
            return frontendUrl + "/dashboard?drive=invalid_state";
        }

        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));

        String successPath = projectId == null
                ? frontendUrl + "/dashboard?drive=connected"
                : frontendUrl + "/dashboard/projects/" + projectId + "/edit?drive=connected";
        String missingPath = projectId == null
                ? frontendUrl + "/dashboard?drive=missing_refresh_token"
                : frontendUrl + "/dashboard/projects/" + projectId + "/edit?drive=missing_refresh_token";

        TokenPayload tokens = exchangeCode(code);
        if (tokens.refreshToken() == null || tokens.refreshToken().isBlank()) {
            // Re-consent may omit refresh_token if already granted; keep existing if present
            DriveConnection existing = driveConnectionRepository.findByMember_Id(memberId).orElse(null);
            if (existing == null) {
                return missingPath;
            }
            existing.updateTokens(null, tokens.email(), expiresAt(tokens.expiresIn()));
            ensureAppRoot(existing);
            return successPath;
        }

        String encrypted = tokenEncryptor.encrypt(tokens.refreshToken());
        DriveConnection connection = driveConnectionRepository.findByMember_Id(memberId)
                .orElseGet(() -> driveConnectionRepository.save(DriveConnection.builder()
                        .member(member)
                        .refreshToken(encrypted)
                        .googleAccountEmail(tokens.email())
                        .build()));
        connection.updateTokens(encrypted, tokens.email(), expiresAt(tokens.expiresIn()));
        ensureAppRoot(connection);
        return successPath;
    }

    @Transactional
    public void disconnect(Long memberId) {
        driveConnectionRepository.deleteByMember_Id(memberId);
    }

    public DriveConnection requireConnection(Long memberId) {
        return driveConnectionRepository.findByMember_Id(memberId)
                .orElseThrow(() -> new BadRequestException("먼저 Google Drive를 연결해 주세요."));
    }

    public String decryptRefreshToken(DriveConnection connection) {
        return tokenEncryptor.decrypt(connection.getRefreshToken());
    }

    public void ensureAppRoot(DriveConnection connection) {
        if (connection.getDriveRootFolderId() != null && !connection.getDriveRootFolderId().isBlank()) {
            return;
        }
        var drive = googleDriveClient.drive(decryptRefreshToken(connection));
        String rootId = googleDriveClient.findOrCreateFolder(drive, "Memory Wedding", null);
        connection.updateDriveRootFolderId(rootId);
    }

    private TokenPayload exchangeCode(String code) {
        try {
            Map<String, String> form = new LinkedHashMap<>();
            form.put("code", code);
            form.put("client_id", clientId);
            form.put("client_secret", clientSecret);
            form.put("redirect_uri", redirectUri);
            form.put("grant_type", "authorization_code");
            String body = form.entrySet().stream()
                    .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                            + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TOKEN_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new BadRequestException("Google Drive 토큰 교환에 실패했습니다.");
            }
            JsonNode json = objectMapper.readTree(response.body());
            String accessToken = text(json, "access_token");
            String refreshToken = text(json, "refresh_token");
            Integer expiresIn = json.has("expires_in") ? json.get("expires_in").asInt() : null;
            String email = fetchEmail(accessToken);
            return new TokenPayload(accessToken, refreshToken, email, expiresIn);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Google Drive 인증에 실패했습니다.");
        }
    }

    private String fetchEmail(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return null;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(USERINFO_URL))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                return null;
            }
            return text(objectMapper.readTree(response.body()), "email");
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime expiresAt(Integer expiresIn) {
        if (expiresIn == null) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.now().plusSeconds(expiresIn), ZoneId.systemDefault());
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private record TokenPayload(String accessToken, String refreshToken, String email, Integer expiresIn) {
    }
}
