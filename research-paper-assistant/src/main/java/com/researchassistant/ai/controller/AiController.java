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
    public ResponseEntity<?> summarize(
            @Valid @RequestBody SummaryRequest request,
            Authentication authentication) {

        String email = authentication.getName();

        // Generate summary using Python AI service
        String result =
                summaryService.generateSummary(
                        request.getProjectId(),
                        request.getDocumentId(),
                        email
                );

        // Extract actual summary text
        String summaryText =
                extractSummaryText(result);

        // Save summary into chat history
        if (summaryText != null &&
                !summaryText.isBlank()) {

            chatService.saveSummaryMessage(
                    request.getProjectId(),
                    request.getDocumentId(),
                    summaryText,
                    email
            );
        }

        // Return original Python response
        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }

    // =========================================================
    // COMPARE TWO PAPERS
    // =========================================================

    @PostMapping(
            value = "/compare",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> comparePapers(
            @Valid @RequestBody CompareRequest request,
            Authentication authentication) {

        String email = authentication.getName();

        // ----------------------------------------------
        // Generate comparison using Python
        // ----------------------------------------------

        String result =
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
                extractComparisonText(result);

        // ----------------------------------------------
        // Save comparison into chat history
        // ----------------------------------------------

        if (comparisonText != null &&
                !comparisonText.isBlank()) {

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

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }

    // =========================================================
    // EXTRACT SUMMARY TEXT
    // =========================================================

    private String extractSummaryText(String result) {

        if (result == null ||
                result.isBlank()) {

            return "";
        }

        try {

            JsonNode node =
                    jsonMapper.readTree(result);

            // Handle JSON string containing JSON
            if (node.isTextual()) {

                node =
                        jsonMapper.readTree(
                                node.asString()
                        );
            }

            // Normal summary response
            if (node.has("summary") &&
                    !node.get("summary").isNull()) {

                return node
                        .get("summary")
                        .asString()
                        .trim();
            }

            // Fallback
            if (node.has("message") &&
                    !node.get("message").isNull()) {

                return node
                        .get("message")
                        .asString()
                        .trim();
            }

        } catch (Exception e) {

            // If response isn't JSON,
            // save the raw response.
            return result.trim();
        }

        return "";
    }

    // =========================================================
    // EXTRACT COMPARISON TEXT
    // =========================================================

    private String extractComparisonText(String result) {

        if (result == null ||
                result.isBlank()) {

            return "";
        }

        try {

            JsonNode node =
                    jsonMapper.readTree(result);

            // Handle JSON string containing JSON
            if (node.isTextual()) {

                node =
                        jsonMapper.readTree(
                                node.asString()
                        );
            }

            // Normal comparison response
            if (node.has("comparison") &&
                    !node.get("comparison").isNull()) {

                return node
                        .get("comparison")
                        .asString()
                        .trim();
            }

            // Fallback
            if (node.has("message") &&
                    !node.get("message").isNull()) {

                return node
                        .get("message")
                        .asString()
                        .trim();
            }

        } catch (Exception e) {

            // If response isn't JSON,
            // save the raw response.
            return result.trim();
        }

        return "";
    }
}