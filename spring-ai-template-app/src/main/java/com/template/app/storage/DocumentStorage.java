package com.template.app.storage;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Abstraction over raw document storage so the template does not hard-code
 * a specific object storage. A local filesystem implementation is provided
 * by default ({@code app.storage.type=local}); implement this interface and
 * register it as a bean (e.g. {@code app.storage.type=minio}) to swap in
 * MinIO/S3 or any other backend.
 */
public interface DocumentStorage {

    /**
     * Stores the raw content of an uploaded file.
     *
     * @param knowledgeId owner knowledge base id (used to group files)
     * @param fileName    original file name (sanitized by the implementation)
     * @param content     raw file content
     * @return the storage path/key that {@link #load} and {@link #delete} accept
     */
    String store(String knowledgeId, String fileName, InputStream content) throws IOException;

    /**
     * Loads a previously stored file as a Spring {@link Resource}.
     */
    Resource load(String storagePath) throws IOException;

    /**
     * Deletes a previously stored file. Missing files are ignored.
     */
    void delete(String storagePath);
}
