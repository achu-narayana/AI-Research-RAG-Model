package com.researchassistant.chat.service;

import com.researchassistant.ai.service.AiService;
import com.researchassistant.chat.dto.ChatMessageRequest;
import com.researchassistant.chat.entity.ChatMessage;
import com.researchassistant.chat.entity.ProjectChat;
import com.researchassistant.chat.repository.ChatMessageRepository;
import com.researchassistant.chat.repository.ProjectChatRepository;
import com.researchassistant.project.entity.Project;
import com.researchassistant.project.repository.ProjectRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatService {

    private final ProjectRepository projectRepository;
    private final ProjectChatRepository projectChatRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AiService aiService;

    public ChatService(
            ProjectRepository projectRepository,
            ProjectChatRepository projectChatRepository,
            ChatMessageRepository chatMessageRepository,
            AiService aiService) {

        this.projectRepository = projectRepository;
        this.projectChatRepository = projectChatRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.aiService = aiService;
    }

    // ==================================================
    // VALIDATE PROJECT ACCESS
    // ==================================================

    private Project validateProject(
            Long projectId,
            String email) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new RuntimeException("Project not found"));

        if (!project.getOwner().getEmail().equals(email)) {

            throw new RuntimeException(
                    "You are not authorized to access this project");
        }

        return project;
    }

    // ==================================================
    // GET OR CREATE PROJECT CHAT
    // ==================================================

    private ProjectChat getOrCreateChat(Project project) {

        return projectChatRepository
                .findByProject(project)
                .orElseGet(() -> {

                    ProjectChat chat = new ProjectChat(
                            project,
                            LocalDateTime.now()
                    );

                    return projectChatRepository.save(chat);
                });
    }

    // ==================================================
    // NORMAL CHAT MESSAGE
    // ==================================================

    @Transactional
    public ChatMessage sendMessage(
            Long projectId,
            ChatMessageRequest request,
            String email) {

        Project project =
                validateProject(projectId, email);

        ProjectChat chat =
                getOrCreateChat(project);

        String documentId =
                request.getDocumentId();

        // ----------------------------------------------
        // Save user message
        // ----------------------------------------------

        ChatMessage userMessage =
                new ChatMessage(
                        chat,
                        "USER",
                        request.getMessage(),
                        documentId,
                        LocalDateTime.now()
                );

        chatMessageRepository.save(userMessage);

        // ----------------------------------------------
        // Ask AI
        // ----------------------------------------------

        String answer =
                aiService.askQuestion(
                        request.getMessage(),
                        projectId,
                        documentId,
                        email
                );

        // ----------------------------------------------
        // Save AI response
        // ----------------------------------------------

        ChatMessage assistantMessage =
                new ChatMessage(
                        chat,
                        "ASSISTANT",
                        answer,
                        documentId,
                        LocalDateTime.now()
                );

        return chatMessageRepository.save(
                assistantMessage
        );
    }

    // ==================================================
    // SAVE SUMMARY TO CHAT HISTORY
    // ==================================================

    @Transactional
    public ChatMessage saveSummaryMessage(
            Long projectId,
            String documentId,
            String summary,
            String email) {

        Project project =
                validateProject(projectId, email);

        ProjectChat chat =
                getOrCreateChat(project);

        ChatMessage summaryMessage =
                new ChatMessage(
                        chat,
                        "ASSISTANT",
                        summary,
                        documentId,
                        LocalDateTime.now()
                );

        return chatMessageRepository.save(
                summaryMessage
        );
    }

    // ==================================================
    // SAVE COMPARISON TO CHAT HISTORY
    // ==================================================

    @Transactional
    public ChatMessage saveComparisonMessage(
            Long projectId,
            String documentId1,
            String documentId2,
            String comparison,
            String email) {

        // Validate project access
        Project project =
                validateProject(projectId, email);

        // Get existing chat or create one
        ProjectChat chat =
                getOrCreateChat(project);

        /*
         * A comparison belongs to TWO papers.
         *
         * ChatMessage currently has only one documentId,
         * so we intentionally save null here instead of
         * pretending the comparison belongs to one paper.
         */

        ChatMessage comparisonMessage =
                new ChatMessage(
                        chat,
                        "ASSISTANT",
                        comparison,
                        null,
                        LocalDateTime.now()
                );

        return chatMessageRepository.save(
                comparisonMessage
        );
    }

    // ==================================================
    // GET CHAT HISTORY
    // ==================================================

    public List<ChatMessage> getChatHistory(
            Long projectId,
            String email) {

        Project project =
                validateProject(projectId, email);

        ProjectChat chat =
                projectChatRepository
                        .findByProject(project)
                        .orElse(null);

        if (chat == null) {
            return List.of();
        }

        return chatMessageRepository
                .findAllByChatOrderByCreatedAtAsc(chat);
    }
}