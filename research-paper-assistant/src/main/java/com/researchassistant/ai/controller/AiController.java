package com.researchassistant.ai.controller;

import com.researchassistant.ai.dto.AskQuestionRequest;
import com.researchassistant.ai.dto.SummaryRequest;
import com.researchassistant.ai.service.AiService;
import com.researchassistant.ai.service.SummaryService;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;
    private final SummaryService summaryService;

    public AiController(
            AiService aiService,
            SummaryService summaryService) {

        this.aiService = aiService;
        this.summaryService = summaryService;
    }

    // ==================================================
    // ASK QUESTION
    // ==================================================

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

    // ==================================================
    // SUMMARY
    // ==================================================

    @PostMapping(
            value = "/summary",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> summarize(
            @Valid @RequestBody SummaryRequest request,
            Authentication authentication) {

        String email = authentication.getName();

        String result =
                summaryService.generateSummary(
                        request.getProjectId(),
                        request.getDocumentId(),
                        email
                );

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }
}