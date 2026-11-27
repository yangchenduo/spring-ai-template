package com.template.app.rag;

/**
 * Metadata keys attached to every chunk written to the vector store.
 *
 * <p>This is the contract the whole RAG pipeline relies on:
 * <ul>
 *   <li>knowledge-base isolation at query time ({@code knowledgeId})</li>
 *   <li>citation / source tracing in answers ({@code docId}, {@code fileName})</li>
 *   <li>cascade cleanup when a document or knowledge base is deleted ({@code docId}, {@code knowledgeId})</li>
 * </ul>
 * Keep writer ({@code KnowledgeServiceImpl}) and reader ({@code RagServiceImpl}) aligned on these keys.
 */
public final class RagMetadata {

    public static final String KNOWLEDGE_ID = "knowledgeId";
    public static final String DOC_ID = "docId";
    public static final String FILE_NAME = "fileName";

    private RagMetadata() {
    }
}
