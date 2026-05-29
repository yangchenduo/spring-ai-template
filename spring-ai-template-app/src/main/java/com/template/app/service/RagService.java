package com.template.app.service;

import com.template.common.dto.RagRequest;
import com.template.common.dto.ChatResponse;
import reactor.core.publisher.Flux;

public interface RagService {

    /**
     * RAG-based question answering (single response)
     */
    ChatResponse ask(RagRequest request);

    /**
     * RAG-based question answering (streaming)
     */
    Flux<String> askStream(RagRequest request);
}
