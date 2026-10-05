package com.researchassistant.chat.controller;

import com.researchassistant.chat.dto.ChatMessageRequest;
import com.researchassistant.chat.dto.ChatMessageResponse;
import com.researchassistant.chat.entity.ChatMessage;
import com.researchassistant.chat.service.ChatPdfService;
import com.researchassistant.chat.service.ChatService;

import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/chat")
public class ChatController {

    private final ChatService chatService;
    private final ChatPdfService chatPdfService;

    public ChatController(
            ChatService chatService,
            ChatPdfService chatPdfService) {

        this.chatService = chatService;
        this.chatPdfService = chatPdfService;
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> downloadChatPdf(
            @PathVariable Long projectId,
            Authentication authentication) {

        String email = authentication.getName();

        byte[] pdf =
                chatPdfService.generateChatPdf(projectId, email);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"research-project-chat.pdf\""
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/messages")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @PathVariable Long projectId,
            @Valid @RequestBody ChatMessageRequest request,
            Authentication authentication) {

        String email = authentication.getName();

        ChatMessage response =
                chatService.sendMessage(
                        projectId,
                        request,
                        email
                );

        return ResponseEntity.ok(
                ChatMessageResponse.fromEntity(response)
        );
    }

    @GetMapping("/messages")
    public ResponseEntity<List<ChatMessageResponse>> getHistory(
            @PathVariable Long projectId,
            Authentication authentication) {

        String email = authentication.getName();

        List<ChatMessage> messages =
                chatService.getChatHistory(
                        projectId,
                        email
                );

        List<ChatMessageResponse> response =
                messages.stream()
                        .map(ChatMessageResponse::fromEntity)
                        .toList();

        return ResponseEntity.ok(response);
    }
}