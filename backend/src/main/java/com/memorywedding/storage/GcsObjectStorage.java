package com.memorywedding.storage;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.memorywedding.common.BadRequestException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "gcs")
public class GcsObjectStorage implements ObjectStorage {

    private final Storage storage;
    private final String bucket;

    public GcsObjectStorage(
            @Value("${app.storage.gcs.bucket}") String bucket,
            @Value("${app.storage.gcs.credentials-path:}") String credentialsPath) {
        this.bucket = bucket;
        if (credentialsPath != null && !credentialsPath.isBlank()) {
            try (InputStream in = new FileInputStream(credentialsPath)) {
                this.storage = StorageOptions.newBuilder()
                        .setCredentials(GoogleCredentials.fromStream(in))
                        .build()
                        .getService();
            } catch (IOException e) {
                throw new IllegalStateException(
                        "GCS 자격증명 파일을 읽을 수 없습니다: " + credentialsPath
                                + " (Docker라면 호스트 폴더를 /secrets 에 마운트하고 "
                                + "GCS_CREDENTIALS_PATH=/secrets/... 로 설정하세요)",
                        e);
            }
        } else {
            this.storage = StorageOptions.getDefaultInstance().getService();
        }
    }

    @Override
    public String provider() {
        return "gcs";
    }

    @Override
    public StoredObject store(String objectKey, InputStream content, long contentLength, String contentType) {
        try {
            BlobInfo info = BlobInfo.newBuilder(BlobId.of(bucket, objectKey))
                    .setContentType(contentType)
                    .build();
            storage.createFrom(info, content);
            return new StoredObject(provider(), objectKey);
        } catch (IOException e) {
            throw new BadRequestException("GCS 업로드에 실패했습니다.");
        }
    }

    @Override
    public InputStream open(String objectKey) {
        var blob = storage.get(BlobId.of(bucket, objectKey));
        if (blob == null || !blob.exists()) {
            throw new BadRequestException("파일을 찾을 수 없습니다.");
        }
        return Channels.newInputStream(blob.reader());
    }

    @Override
    public void delete(String objectKey) {
        storage.delete(BlobId.of(bucket, objectKey));
    }
}
