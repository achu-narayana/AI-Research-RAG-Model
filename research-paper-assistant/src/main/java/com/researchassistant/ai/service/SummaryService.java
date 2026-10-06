package com.researchassistant.ai.service;

import com.researchassistant.ai.client.AiHttpClient;
import com.researchassistant.common.exception.AiServiceException;

import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SummaryService {

    private final AiHttpClient aiHttpClient;
    private final AiAccessService aiAccessService;

    public SummaryService(
            AiHttpClient aiHttpClient,
            AiAccessService aiAccessService) {

        this.aiHttpClient = aiHttpClient;
        this.aiAccessService = aiAccessService;
    }

    /**
     * Returns the Python service's JSON response unchanged.
     */
    public JsonNode generateSummary(
            Long projectId,
            String documentId,
            String email) {

        // -----------------------------------------
        // 1. Validate user access
        // -----------------------------------------

        aiAccessService.validateAccess(
                projectId,
                documentId,
                email
        );

        // -----------------------------------------
        // 2. Build request body
        // -----------------------------------------

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("project_id", projectId);

        // document_id is optional
        if (documentId != null && !documentId.isBlank()) {
            body.put("document_id", documentId);
        }

        // -----------------------------------------
        // 3. Call Python
        // -----------------------------------------

        JsonNode response =
                aiHttpClient.postJson("/api/ai/summary", body);

        if (response == null) {
            throw new AiServiceException(
                    "AI service returned an empty summary response");
        }

        return response;
    }
}
