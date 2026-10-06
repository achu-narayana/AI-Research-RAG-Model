package com.researchassistant.chat.service;

import com.researchassistant.ai.service.AiAccessService;
import com.researchassistant.ai.service.AiService;
import com.researchassistant.chat.dto.ChatMessageRequest;
import com.researchassistant.chat.entity.ChatMessage;
import com.researchassistant.chat.entity.ProjectChat;
import com.researchassistant.chat.repository.ChatMessageRepository;
import com.researchassistant.chat.repository.ProjectChatRepository;
import com.researchassistant.project.entity.Project;
import com.researchassistant.project.service.ProjectAccessService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatService {

    private final ProjectAccessService projectAccessService;
    private final ProjectChatRepository projectChatRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AiService aiService;
    private final AiAccessService aiAccessService;
    private final TransactionTemplate transactionTemplate;

    public ChatService(
            ProjectAccessService projectAccessService,
            ProjectChatRepository projectChatRepository,
            ChatMessageRepository chatMessageRepository,
            AiService aiService,
            AiAccessService aiAccessService,
            PlatformTransactionManager transactionManager) {

        this.projectAccessService = projectAccessService;
        this.projectChatRepository = projectChatRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.aiService = aiService;
        this.aiAccessService = aiAccessService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
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

    /**
     * Not transactional on purpose: the AI call can take minutes,
     * so the user message and the answer are saved in two short
     * transactions around it.
     */
    public ChatMessage sendMessage(
            Long projectId,
            ChatMessageRequest request,
            String email) {

        String documentId =
                request.getDocumentId() == null ||
                        request.getDocumentId().isBlank()
                        ? null
                        : request.getDocumentId();

        // Fail fast (404/403) before anything is saved
        aiAccessService.validateAccess(projectId, documentId, email);

        // ----------------------------------------------
        // 1. Save user message (tx)
        // ----------------------------------------------

        ChatMessage userMessage = transactionTemplate.execute(status -> {

            Project project =
                    projectAccessService.getOwnedProject(projectId, email);

            ProjectChat chat =
                    getOrCreateChat(project);

            return chatMessageRepository.save(
                    new ChatMessage(
                            chat,
                            "USER",
                            request.getMessage(),
                            documentId,
                            LocalDateTime.now()
                    )
            );
        });

        // ----------------------------------------------
        // 2. Ask AI (no tx)
        // ----------------------------------------------

        String answer;

        try {
            answer = aiService.askQuestion(
                    request.getMessage(),
                    projectId,
                    documentId,
                    email
            );

        } catch (RuntimeException e) {
            // Don't leave an unanswered question in the history
            chatMessageRepository.deleteById(userMessage.getId());
            throw e;
        }

        // ----------------------------------------------
        // 3. Save AI response (tx)
        // ----------------------------------------------

        ChatMessage assistantMessage =
                new ChatMessage(
                        userMessage.getChat(),
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
                projectAccessService.getOwnedProject(projectId, email);

        ProjectChat chat =
                getOrCreateChat(project);

        ChatMessage summaryMessage =
                new ChatMessage(
                        chat,
                        "ASSISTANT",
                        summary,
                        documentId == null || documentId.isBlank()
                                ? null
                                : documentId,
                        null,
                        ChatMessage.TYPE_SUMMARY,
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
                projectAccessService.getOwnedProject(projectId, email);

        // Get existing chat or create one
        ProjectChat chat =
                getOrCreateChat(project);

        ChatMessage comparisonMessage =
                new ChatMessage(
                        chat,
                        "ASSISTANT",
                        comparison,
                        documentId1,
                        documentId2,
                        ChatMessage.TYPE_COMPARISON,
                        LocalDateTime.now()
                );

        return chatMessageRepository.save(
                comparisonMessage
        );
    }

    // ==================================================
    // GET CHAT HISTORY
    // ==================================================

    @Transactional(readOnly = true)
    public List<ChatMessage> getChatHistory(
            Long projectId,
            String email) {

        Project project =
                projectAccessService.getOwnedProject(projectId, email);

        ProjectChat chat =
                projectChatRepository
                        .findByProject(project)
                        .orElse(null);

        if (chat == null) {
            return List.of();
        }

        return chatMessageRepository
                .findAllByChatOrderByIdAsc(chat);
    }
}
