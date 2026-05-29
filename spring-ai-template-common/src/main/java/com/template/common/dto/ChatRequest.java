package com.template.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class ChatRequest implements Serializable {

    @NotBlank(message = "message must not be blank")
    @Size(max = 4096, message = "message must not exceed 4096 characters")
    private String message;

    /**
     * Conversation ID for multi-turn chat. Null for new conversation.
     */
    private String conversationId;

    /**
     * AI provider to use: ollama / openai. Default: ollama
     */
    private String provider;

    /**
     * Model name override. If null, uses default from config.
     */
    private String model;

    /**
     * Temperature for generation (0.0 - 2.0)
     */
    private Double temperature;
}
