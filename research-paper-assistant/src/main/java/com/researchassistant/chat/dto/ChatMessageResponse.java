package com.researchassistant.chat.dto;

import com.researchassistant.chat.entity.ChatMessage;

import java.time.LocalDateTime;

public class ChatMessageResponse {

    private Long id;
    private String role;
    private String content;
    private String documentId;
    private String secondDocumentId;
    private String messageType;
    private LocalDateTime createdAt;

    public ChatMessageResponse(
            Long id,
            String role,
            String content,
            String documentId,
            String secondDocumentId,
            String messageType,
            LocalDateTime createdAt) {

        this.id = id;
        this.role = role;
        this.content = content;
        this.documentId = documentId;
        this.secondDocumentId = secondDocumentId;
        this.messageType = messageType;
        this.createdAt = createdAt;
    }

    public static ChatMessageResponse fromEntity(ChatMessage message) {

        String messageType = message.getMessageType();

        // Rows created before message_type existed
        if (messageType == null || messageType.isBlank()) {
            messageType = ChatMessage.TYPE_CHAT;
        }

        return new ChatMessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getDocumentId(),
                message.getSecondDocumentId(),
                messageType,
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

    public String getSecondDocumentId() {
        return secondDocumentId;
    }

    public String getMessageType() {
        return messageType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
