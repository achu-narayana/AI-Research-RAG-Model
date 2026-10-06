package com.researchassistant.ai.controller;

import com.researchassistant.ai.dto.AskQuestionRequest;
import com.researchassistant.ai.dto.CompareRequest;
import com.researchassistant.ai.dto.SummaryRequest;
import com.researchassistant.ai.service.AiService;
import com.researchassistant.ai.service.CompareService;
import com.researchassistant.ai.service.SummaryService;
import com.researchassistant.chat.service.ChatService;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;
    private final SummaryService summaryService;
    private final CompareService compareService;
    private final ChatService chatService;
    private final JsonMapper jsonMapper;

    public AiController(
            AiService aiService,
            SummaryService summaryService,
            CompareService compareService,
            ChatService chatService,
            JsonMapper jsonMapper) {

        this.aiService = aiService;
        this.summaryService = summaryService;
        this.compareService = compareService;
        this.chatService = chatService;
        this.jsonMapper = jsonMapper;
    }

    // =========================================================
    // ASK QUESTION
    // =========================================================

    @PostMapping("/ask")
    public ResponseEntity<?> askQuestion(
            @Valid @RequestBody AskQuestionRequest request,
            Authentication authentication) {

        String email = authentication.getName();

        String answer = aiService.askQuestion(
                request.getQuestion(),
                request.getProjectId(),
                request.getDocumentId(),
                email
        );

        return ResponseEntity.ok(
                Map.of(
                        "question",
                        request.getQuestion(),

                        "projectId",
                        request.getProjectId(),

                        "documentId",
                        request.getDocumentId() == null
                                ? "ALL_PAPERS"
                                : request.getDocumentId(),

                        "answer",
                        answer
                )
        );
    }

    // =========================================================
    // GENERATE SUMMARY
    // =========================================================

    @PostMapping(
            value = "/summary",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<JsonNode> summarize(
            @Valid @RequestBody SummaryRequest request,
            Authentication authentication) {

        String email = authentication.getName();

        // Generate summary using Python AI service
        JsonNode result =
                summaryService.generateSummary(
                        request.getProjectId(),
                        request.getDocumentId(),
                        email
                );

        // Extract actual summary text
        String summaryText =
                extractText(result, "summary");

        // Save summary into chat history
        if (!summaryText.isBlank()) {

            chatService.saveSummaryMessage(
                    request.getProjectId(),
                    request.getDocumentId(),
                    summaryText,
                    email
            );
        }

        // Return original Python response
        return ResponseEntity.ok(result);
    }

    // =========================================================
    // COMPARE TWO PAPERS
    // =========================================================

    @PostMapping(
            value = "/compare",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<JsonNode> comparePapers(
            @Valid @RequestBody CompareRequest request,
            Authentication authentication) {

        String email = authentication.getName();

        // ----------------------------------------------
        // Generate comparison using Python
        // ----------------------------------------------

        JsonNode result =
                compareService.comparePapers(
                        request.getProjectId(),
                        request.getDocumentId1(),
                        request.getDocumentId2(),
                        email
                );

        // ----------------------------------------------
        // Extract comparison text
        // ----------------------------------------------

        String comparisonText =
                extractText(result, "comparison");

        // ----------------------------------------------
        // Save comparison into chat history
        // ----------------------------------------------

        if (!comparisonText.isBlank()) {

            chatService.saveComparisonMessage(
                    request.getProjectId(),
                    request.getDocumentId1(),
                    request.getDocumentId2(),
                    comparisonText,
                    email
            );
        }

        // ----------------------------------------------
        // Return original Python response
        // ----------------------------------------------

        return ResponseEntity.ok(result);
    }

    // =========================================================
    // EXTRACT TEXT FIELD FROM PYTHON RESPONSE
    // =========================================================

    private String extractText(JsonNode node, String field) {

        if (node == null || node.isNull()) {
            return "";
        }

        // Handle a JSON string that itself contains JSON
        if (node.isString()) {
            try {
                node = jsonMapper.readTree(node.asString());

            } catch (RuntimeException e) {
                return node.asString().trim();
            }
        }

        if (node.hasNonNull(field)) {
            return node.get(field).asString().trim();
        }

        // Fallback
        if (node.hasNonNull("message")) {
            return node.get("message").asString().trim();
        }

        return "";
    }
}
