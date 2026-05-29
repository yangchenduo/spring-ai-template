package com.template.common.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class ChatResponse implements Serializable {

    private String conversationId;
    private String message;
    private String provider;
    private String model;
    private long elapsed; // ms
}
