package com.template.app.service.impl;

import com.template.app.entity.ChatHistoryEntity;
import com.template.app.mapper.ChatHistoryMapper;
import com.template.app.service.ChatService;
import com.template.common.dto.ChatRequest;
import com.template.common.dto.ChatResponse;
import com.template.common.utils.ConversationIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final ChatHistoryMapper chatHistoryMapper;

    private final Map<String, List<Message>> conversationStore = new ConcurrentHashMap<>();

    private static final String SYSTEM_PROMPT = """
            You are a helpful AI assistant. Answer questions concisely and accurately.
            If you don't know the answer, say so honestly.
            Current date: {date}
            """;

    @Override
    public ChatResponse chat(ChatRequest request) {
        String conversationId = request.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = ConversationIdGenerator.generate();
        }

        long start = System.currentTimeMillis();

        String systemPrompt = new SystemPromptTemplate(SYSTEM_PROMPT)
                .render(Map.of("date", LocalDateTime.now().toLocalDate().toString()));

        List<Message> history = conversationStore.computeIfAbsent(conversationId, k -> new ArrayList<>());
        history.add(new UserMessage(request.getMessage()));

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.addAll(history);

        String response = chatClient.prompt()
                .messages(messages)
                .call()
                .content();

        history.add(new AssistantMessage(response));

        saveHistory(conversationId, "user", request.getMessage(), "ollama", null);
        saveHistory(conversationId, "assistant", response, "ollama", null);

        long elapsed = System.currentTimeMillis() - start;

        return ChatResponse.builder()
                .conversationId(conversationId)
                .message(response)
                .provider("ollama")
                .model(null)
                .elapsed(elapsed)
                .build();
    }

    @Override
    public Flux<String> chatStream(ChatRequest request) {
        String conversationId = request.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = ConversationIdGenerator.generate();
        }

        List<Message> history = conversationStore.computeIfAbsent(conversationId, k -> new ArrayList<>());
        history.add(new UserMessage(request.getMessage()));

        String systemPrompt = new SystemPromptTemplate(SYSTEM_PROMPT)
                .render(Map.of("date", LocalDateTime.now().toLocalDate().toString()));

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.addAll(history);

        return chatClient.prompt()
                .messages(messages)
                .stream()
                .content();
    }

    private void saveHistory(String conversationId, String role, String content, String provider, String model) {
        try {
            ChatHistoryEntity entity = new ChatHistoryEntity();
            entity.setConversationId(conversationId);
            entity.setRole(role);
            entity.setContent(content);
            entity.setProvider(provider);
            entity.setModel(model);
            entity.setCreatedAt(LocalDateTime.now());
            chatHistoryMapper.insert(entity);
        } catch (Exception e) {
            log.warn("Failed to save chat history: {}", e.getMessage());
        }
    }
}
