package com.memorywedding.storage;

import com.memorywedding.common.BadRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalObjectStorage implements ObjectStorage {

    private final Path root;

    public LocalObjectStorage(@Value("${app.storage.local-path:./data/uploads}") String localPath) {
        this.root = Path.of(localPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.root);
        } catch (IOException e) {
            throw new IllegalStateException("업로드 로컬 저장 경로를 만들 수 없습니다: " + this.root, e);
        }
    }

    @Override
    public String provider() {
        return "local";
    }

    @Override
    public StoredObject store(String objectKey, InputStream content, long contentLength, String contentType) {
        Path target = resolve(objectKey);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredObject(provider(), objectKey);
        } catch (IOException e) {
            throw new BadRequestException("파일 저장에 실패했습니다.");
        }
    }

    @Override
    public InputStream open(String objectKey) {
        try {
            return Files.newInputStream(resolve(objectKey));
        } catch (IOException e) {
            throw new BadRequestException("파일을 찾을 수 없습니다.");
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            Files.deleteIfExists(resolve(objectKey));
        } catch (IOException e) {
            // ignore missing files on soft-delete
        }
    }

    private Path resolve(String objectKey) {
        Path resolved = root.resolve(objectKey).normalize();
        if (!resolved.startsWith(root)) {
            throw new BadRequestException("잘못된 저장 경로입니다.");
        }
        return resolved;
    }
}
