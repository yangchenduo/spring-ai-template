package com.template.app.service;

import com.template.common.dto.ChatRequest;
import com.template.common.dto.ChatResponse;
import reactor.core.publisher.Flux;

public interface ChatService {

    /**
     * Single-turn chat (wait for full response)
     */
    ChatResponse chat(ChatRequest request);

    /**
     * Streaming chat (SSE)
     */
    Flux<String> chatStream(ChatRequest request);
}
