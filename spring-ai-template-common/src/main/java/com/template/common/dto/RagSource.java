package com.template.common.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * A retrieval hit that grounded a RAG answer, exposed for citation/tracing in the UI.
 */
@Data
@Builder
public class RagSource implements Serializable {

    /** Id of the source document (knowledge_doc.id). */
    private String docId;

    /** Id of the knowledge base the chunk belongs to. */
    private String knowledgeId;

    /** Original file name of the source document. */
    private String fileName;

    /** Similarity score of the retrieved chunk (higher is more similar). */
    private Double score;

    /** Short excerpt of the retrieved chunk. */
    private String snippet;
}
