package com.template.app.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Fails fast when the embedding model and the vector store disagree on
 * dimensions - the most common (and most confusing) misconfiguration when
 * swapping embedding providers.
 *
 * <p>On a mismatch the application refuses to start with a message that says
 * exactly which side to fix. When the probe itself cannot reach the embedding
 * endpoint (missing key, network) the check only logs an error and lets the
 * application start, since the failure will surface on first use anyway.
 */
@Slf4j
@Component
public class EmbeddingDimensionCheck implements ApplicationRunner {

    private final EmbeddingModel embeddingModel;
    private final int vectorDimensions;

    public EmbeddingDimensionCheck(EmbeddingModel embeddingModel,
            @Value("${spring.ai.vectorstore.pgvector.dimensions:1024}") int vectorDimensions) {
        this.embeddingModel = embeddingModel;
        this.vectorDimensions = vectorDimensions;
    }

    @Override
    public void run(ApplicationArguments args) {
        float[] probe;
        try {
            probe = embeddingModel.embed("embedding dimension check");
        } catch (Exception e) {
            log.error("Embedding probe failed - check EMBEDDING_BASE_URL / EMBEDDING_API_KEY / EMBEDDING_MODEL. "
                    + "Starting anyway, but ingestion and RAG retrieval will fail until this is fixed. "
                    + "Cause: {}", e.getMessage());
            return;
        }
        if (probe.length != vectorDimensions) {
            throw new IllegalStateException(String.format(
                    "Embedding model returns %d dimensions but the vector store is configured for %d. "
                            + "Set EMBEDDING_DIMENSIONS=%d (and recreate the vector_store table if it already exists - "
                            + "switching embedding models always requires re-ingestion).",
                    probe.length, vectorDimensions, probe.length));
        }
        log.info("Embedding check passed: model returns {} dimensions, vector store configured for {}",
                probe.length, vectorDimensions);
    }
}
