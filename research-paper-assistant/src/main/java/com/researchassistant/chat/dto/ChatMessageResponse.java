package com.researchassistant.chat.dto;

import com.researchassistant.chat.entity.ChatMessage;

import java.time.LocalDateTime;

public class ChatMessageResponse {

    private Long id;
    private String role;
    private String content;
    private String documentId;
    private LocalDateTime createdAt;

    public ChatMessageResponse(
            Long id,
            String role,
            String content,
            String documentId,
            LocalDateTime createdAt) {

        this.id = id;
        this.role = role;
        this.content = content;
        this.documentId = documentId;
        this.createdAt = createdAt;
    }

    public static ChatMessageResponse fromEntity(ChatMessage message) {

        return new ChatMessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getDocumentId(),
                message.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public String getDocumentId() {
        return documentId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}