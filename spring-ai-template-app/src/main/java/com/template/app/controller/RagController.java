package com.template.app.controller;

import com.template.app.service.RagService;
import com.template.common.dto.ChatResponse;
import com.template.common.dto.RagRequest;
import com.template.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@Tag(name = "RAG", description = "RAG (Retrieval-Augmented Generation) API - knowledge-based QA")
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    @Operation(summary = "Ask a question using RAG (synchronous)")
    @PostMapping
    public Result<ChatResponse> ask(@Valid @RequestBody RagRequest request) {
        return Result.ok(ragService.ask(request));
    }

    @Operation(summary = "Ask a question using RAG (streaming SSE)")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> askStream(@Valid @RequestBody RagRequest request) {
        return ragService.askStream(request);
    }
}
