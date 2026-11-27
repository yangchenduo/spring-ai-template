package com.template.app.service.impl;

import com.template.app.rag.RagMetadata;
import com.template.app.service.RagService;
import com.template.common.dto.ChatResponse;
import com.template.common.dto.RagRequest;
import com.template.common.dto.RagSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * RAG pipeline: explicit retrieval with knowledge-base isolation, then a
 * grounded prompt, then an answer with source citations.
 *
 * <p>Retrieval is done explicitly (instead of via QuestionAnswerAdvisor) so
 * that the sync and streaming paths share one code path and the retrieved
 * chunks are first-class data for citation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private static final int DEFAULT_TOP_K = 5;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.7;
    private static final int SNIPPET_LENGTH = 200;

    /** Knowledge base ids are server-generated; this guards the filter expression. */
    private static final Pattern SAFE_KB_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private static final String SYSTEM_PROMPT = """
            You are a helpful assistant that answers questions strictly based on the context documents provided below.
            Rules:
            - Answer ONLY from the context documents. If they do not contain the answer, say so honestly.
            - Cite the source file name(s) like [file.pdf] next to the statements you use.
            - Answer in the same language as the question, concisely.
            Current date: {date}
            """;

    private static final String NO_MATCH_ANSWER =
            "I could not find relevant information in the selected knowledge base(s) to answer this question.";

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    @Override
    public ChatResponse ask(RagRequest request) {
        long start = System.currentTimeMillis();
        List<Document> docs = retrieve(request);

        String answer = docs.isEmpty() ? NO_MATCH_ANSWER : callModel(request.getQuestion(), docs);

        return ChatResponse.builder()
                .conversationId("rag_" + System.currentTimeMillis())
                .message(answer)
                .provider("rag")
                .elapsed(System.currentTimeMillis() - start)
                .sources(toSources(docs))
                .build();
    }

    @Override
    public Flux<String> askStream(RagRequest request) {
        List<Document> docs = retrieve(request);
        if (docs.isEmpty()) {
            return Flux.just(NO_MATCH_ANSWER);
        }
        return chatClient.prompt()
                .system(systemPrompt())
                .user(userPrompt(request.getQuestion(), docs))
                .stream()
                .content();
    }

    // ---------------------------------------------------------------
    // retrieval
    // ---------------------------------------------------------------

    private List<Document> retrieve(RagRequest request) {
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(request.getQuestion())
                .topK(request.getTopK() != null ? request.getTopK() : DEFAULT_TOP_K)
                .similarityThreshold(request.getSimilarityThreshold() != null
                        ? request.getSimilarityThreshold() : DEFAULT_SIMILARITY_THRESHOLD);
        String filter = knowledgeFilter(request.getKnowledgeIds());
        if (filter != null) {
            builder.filterExpression(filter);
        }
        List<Document> docs = vectorStore.similaritySearch(builder.build());
        log.debug("RAG retrieval: {} hit(s), filter=[{}]", docs.size(), filter);
        return docs;
    }

    /**
     * Builds a metadata filter that restricts retrieval to the requested
     * knowledge bases, e.g. {@code knowledgeId in ['a', 'b']}.
     * Returns null to search across all knowledge bases.
     */
    private String knowledgeFilter(List<String> knowledgeIds) {
        if (knowledgeIds == null || knowledgeIds.isEmpty()) {
            return null;
        }
        String ids = knowledgeIds.stream()
                .peek(this::requireSafeId)
                .map(id -> "'" + id + "'")
                .collect(Collectors.joining(", "));
        return RagMetadata.KNOWLEDGE_ID + " in [" + ids + "]";
    }

    private void requireSafeId(String id) {
        if (id == null || !SAFE_KB_ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Invalid knowledge base id: " + id);
        }
    }

    // ---------------------------------------------------------------
    // generation
    // ---------------------------------------------------------------

    private String callModel(String question, List<Document> docs) {
        return chatClient.prompt()
                .system(systemPrompt())
                .user(userPrompt(question, docs))
                .call()
                .content();
    }

    private String systemPrompt() {
        return SYSTEM_PROMPT.replace("{date}", LocalDateTime.now().toLocalDate().toString());
    }

    private String userPrompt(String question, List<Document> docs) {
        StringBuilder sb = new StringBuilder("Context documents:\n\n");
        for (Document doc : docs) {
            sb.append("--- [source: ").append(str(doc.getMetadata().get(RagMetadata.FILE_NAME)))
                    .append(" | docId: ").append(str(doc.getMetadata().get(RagMetadata.DOC_ID)))
                    .append("]\n")
                    .append(doc.getText())
                    .append("\n\n");
        }
        sb.append("Question: ").append(question);
        return sb.toString();
    }

    private List<RagSource> toSources(List<Document> docs) {
        return docs.stream()
                .map(doc -> RagSource.builder()
                        .docId(str(doc.getMetadata().get(RagMetadata.DOC_ID)))
                        .knowledgeId(str(doc.getMetadata().get(RagMetadata.KNOWLEDGE_ID)))
                        .fileName(str(doc.getMetadata().get(RagMetadata.FILE_NAME)))
                        .score(doc.getScore())
                        .snippet(snippet(doc.getText()))
                        .build())
                .toList();
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private String str(Object value) {
        return value == null ? null : value.toString();
    }

    private String snippet(String text) {
        if (text == null || text.isEmpty()) return "";
        return text.length() <= SNIPPET_LENGTH ? text : text.substring(0, SNIPPET_LENGTH) + "...";
    }
}
