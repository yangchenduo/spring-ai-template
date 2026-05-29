package com.template.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AIProvider {

    OLLAMA("ollama", "Local Ollama"),
    OPENAI("openai", "OpenAI / Compatible"),
    ;

    private final String key;
    private final String description;
}
