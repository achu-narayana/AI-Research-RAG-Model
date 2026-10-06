package com.researchassistant.chat.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    public static final String TYPE_CHAT = "CHAT";
    public static final String TYPE_SUMMARY = "SUMMARY";
    public static final String TYPE_COMPARISON = "COMPARISON";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_id", nullable = false)
    private ProjectChat chat;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "document_id")
    private String documentId;

    // CHAT, SUMMARY or COMPARISON.
    // The DB default lets ddl-auto=update add the column to
    // existing rows; ChatMessageResponse also maps null -> CHAT.
    @Column(
            name = "message_type",
            nullable = false,
            length = 20,
            columnDefinition = "varchar(20) default 'CHAT'"
    )
    private String messageType = TYPE_CHAT;

    // Only set for COMPARISON messages
    @Column(name = "second_document_id")
    private String secondDocumentId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public ChatMessage() {
    }

    public ChatMessage(
            ProjectChat chat,
            String role,
            String content,
            String documentId,
            LocalDateTime createdAt) {

        this(chat, role, content, documentId, null, TYPE_CHAT, createdAt);
    }

    public ChatMessage(
            ProjectChat chat,
            String role,
            String content,
            String documentId,
            String secondDocumentId,
            String messageType,
            LocalDateTime createdAt) {

        this.chat = chat;
        this.role = role;
        this.content = content;
        this.documentId = documentId;
        this.secondDocumentId = secondDocumentId;
        this.messageType = messageType;
        this.createdAt = createdAt;
    }

    @PrePersist
    void applyDefaults() {
        if (messageType == null || messageType.isBlank()) {
            messageType = TYPE_CHAT;
        }
    }

    public Long getId() {
        return id;
    }

    public ProjectChat getChat() {
        return chat;
    }

    public void setChat(ProjectChat chat) {
        this.chat = chat;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getSecondDocumentId() {
        return secondDocumentId;
    }

    public void setSecondDocumentId(String secondDocumentId) {
        this.secondDocumentId = secondDocumentId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
