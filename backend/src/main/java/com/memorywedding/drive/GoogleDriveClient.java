package com.memorywedding.drive;

import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.UserCredentials;
import com.memorywedding.common.BadRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GoogleDriveClient {

    private static final NetHttpTransport TRANSPORT = new NetHttpTransport();
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private final String clientId;
    private final String clientSecret;

    public GoogleDriveClient(
            @Value("${spring.security.oauth2.client.registration.google.client-id}") String clientId,
            @Value("${spring.security.oauth2.client.registration.google.client-secret}") String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public Drive drive(String refreshToken) {
        UserCredentials credentials = UserCredentials.newBuilder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRefreshToken(refreshToken)
                .build();
        HttpRequestInitializer initializer = new HttpCredentialsAdapter(credentials);
        return new Drive.Builder(TRANSPORT, JSON_FACTORY, initializer)
                .setApplicationName("Memory Wedding")
                .build();
    }

    public String findOrCreateFolder(Drive drive, String name, String parentId) {
        try {
            String q = "mimeType = 'application/vnd.google-apps.folder'"
                    + " and name = '" + escape(name) + "'"
                    + " and trashed = false";
            if (parentId != null) {
                q += " and '" + parentId + "' in parents";
            } else {
                q += " and 'root' in parents";
            }
            FileList list = drive.files().list()
                    .setQ(q)
                    .setSpaces("drive")
                    .setFields("files(id, name)")
                    .setPageSize(1)
                    .execute();
            List<File> files = list.getFiles();
            if (files != null && !files.isEmpty()) {
                return files.get(0).getId();
            }

            File meta = new File();
            meta.setName(name);
            meta.setMimeType("application/vnd.google-apps.folder");
            if (parentId != null) {
                meta.setParents(Collections.singletonList(parentId));
            }
            File created = drive.files().create(meta)
                    .setFields("id")
                    .execute();
            return created.getId();
        } catch (IOException e) {
            throw new BadRequestException("Google Drive 폴더 생성에 실패했습니다.");
        }
    }

    public String uploadFile(
            Drive drive,
            String parentFolderId,
            String filename,
            String mimeType,
            InputStream content,
            long contentLength) {
        try {
            File meta = new File();
            meta.setName(filename);
            meta.setParents(Collections.singletonList(parentFolderId));
            InputStreamContent media = new InputStreamContent(mimeType, content);
            if (contentLength > 0) {
                media.setLength(contentLength);
            }
            File created = drive.files().create(meta, media)
                    .setFields("id")
                    .execute();
            return created.getId();
        } catch (IOException e) {
            throw new BadRequestException("Google Drive 업로드에 실패했습니다.");
        }
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }
}
