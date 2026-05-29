package com.template.app.service.impl;

import com.template.app.service.RagService;
import com.template.common.dto.ChatResponse;
import com.template.common.dto.RagRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@Slf4j
@Service
public class RagServiceImpl implements RagService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public RagServiceImpl(ChatClient chatClient, VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
    }

    private static final String RAG_SYSTEM_PROMPT = """
            You are a helpful assistant that answers questions based on the provided context.
            If the context doesn't contain enough information to answer, say so honestly.
            Always cite the source document when possible.
            Current date: {date}
            """;

    @Override
    public ChatResponse ask(RagRequest request) {
        long start = System.currentTimeMillis();

        int topK = request.getTopK() != null ? request.getTopK() : 5;
        double threshold = request.getSimilarityThreshold() != null ? request.getSimilarityThreshold() : 0.7;

        SearchRequest searchRequest = SearchRequest.builder()
                .query(request.getQuestion())
                .topK(topK)
                .similarityThreshold(threshold)
                .build();

        String systemPrompt = RAG_SYSTEM_PROMPT.replace("{date}",
                LocalDateTime.now().toLocalDate().toString());

        String response = chatClient.prompt()
                .system(systemPrompt)
                .user(request.getQuestion())
                .advisors(new QuestionAnswerAdvisor(vectorStore, searchRequest))
                .call()
                .content();

        long elapsed = System.currentTimeMillis() - start;

        return ChatResponse.builder()
                .conversationId("rag_" + System.currentTimeMillis())
                .message(response)
                .provider("rag")
                .elapsed(elapsed)
                .build();
    }

    @Override
    public Flux<String> askStream(RagRequest request) {
        int topK = request.getTopK() != null ? request.getTopK() : 5;
        double threshold = request.getSimilarityThreshold() != null ? request.getSimilarityThreshold() : 0.7;

        SearchRequest searchRequest = SearchRequest.builder()
                .query(request.getQuestion())
                .topK(topK)
                .similarityThreshold(threshold)
                .build();

        String systemPrompt = RAG_SYSTEM_PROMPT.replace("{date}",
                LocalDateTime.now().toLocalDate().toString());

        return chatClient.prompt()
                .system(systemPrompt)
                .user(request.getQuestion())
                .advisors(new QuestionAnswerAdvisor(vectorStore, searchRequest))
                .stream()
                .content();
    }
}
