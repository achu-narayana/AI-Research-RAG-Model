package com.researchassistant.ai.service;

import com.researchassistant.ai.client.AiHttpClient;
import com.researchassistant.common.exception.AiServiceException;
import com.researchassistant.common.exception.BadRequestException;

import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CompareService {

    private final AiHttpClient aiHttpClient;
    private final AiAccessService aiAccessService;

    public CompareService(
            AiHttpClient aiHttpClient,
            AiAccessService aiAccessService) {

        this.aiHttpClient = aiHttpClient;
        this.aiAccessService = aiAccessService;
    }

    /**
     * Returns the Python service's JSON response unchanged.
     */
    public JsonNode comparePapers(
            Long projectId,
            String documentId1,
            String documentId2,
            String email) {

        // ----------------------------------------------
        // Check that two different papers were selected
        // ----------------------------------------------

        if (documentId1.equals(documentId2)) {
            throw new BadRequestException(
                    "Please select two different research papers"
            );
        }

        // ----------------------------------------------
        // Validate access to both papers
        // ----------------------------------------------

        aiAccessService.validateAccess(
                projectId,
                documentId1,
                email
        );

        aiAccessService.validateAccess(
                projectId,
                documentId2,
                email
        );

        // ----------------------------------------------
        // Build request and call Python
        // ----------------------------------------------

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("project_id", projectId);
        body.put("document_id_1", documentId1);
        body.put("document_id_2", documentId2);

        JsonNode response =
                aiHttpClient.postJson("/api/ai/compare", body);

        if (response == null) {
            throw new AiServiceException(
                    "AI service returned an empty comparison response");
        }

        return response;
    }
}
