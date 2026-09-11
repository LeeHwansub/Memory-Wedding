package com.memorywedding.storage;

import java.io.InputStream;

public interface ObjectStorage {

    String provider();

    StoredObject store(String objectKey, InputStream content, long contentLength, String contentType);

    InputStream open(String objectKey);

    void delete(String objectKey);

    record StoredObject(String provider, String objectKey) {
    }
}
