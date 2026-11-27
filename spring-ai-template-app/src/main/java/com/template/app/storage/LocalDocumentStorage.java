package com.template.app.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Default {@link DocumentStorage} that keeps raw files on the local filesystem,
 * one directory per knowledge base. Activate with {@code app.storage.type=local}
 * (the default). For containerized deployments mount {@code app.storage.local.root}
 * as a volume.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalDocumentStorage implements DocumentStorage {

    private final Path root;

    public LocalDocumentStorage(@Value("${app.storage.local.root:./data/documents}") String root) throws IOException {
        this.root = Path.of(root).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
        log.info("Local document storage root: {}", this.root);
    }

    @Override
    public String store(String knowledgeId, String fileName, InputStream content) throws IOException {
        String key = knowledgeId + "/" + UUID.randomUUID() + "_" + sanitize(fileName);
        Path target = resolve(key);
        Files.createDirectories(target.getParent());
        Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        return key;
    }

    @Override
    public Resource load(String storagePath) throws IOException {
        Path path = resolve(storagePath);
        if (!Files.exists(path)) {
            throw new IOException("Stored file not found: " + path);
        }
        return new FileSystemResource(path);
    }

    @Override
    public void delete(String storagePath) {
        try {
            Files.deleteIfExists(resolve(storagePath));
        } catch (IOException e) {
            log.warn("Failed to delete stored file {}: {}", storagePath, e.getMessage());
        }
    }

    /**
     * Resolves a storage key below the root and guards against path traversal.
     */
    private Path resolve(String storagePath) {
        Path path = root.resolve(storagePath).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Illegal storage path: " + storagePath);
        }
        return path;
    }

    private static String sanitize(String fileName) {
        String cleaned = (fileName == null ? "document" : fileName)
                .replaceAll("[^A-Za-z0-9._-]", "_")
                .replaceAll("^\\.+", "");
        return cleaned.isBlank() ? "document" : cleaned;
    }
}
