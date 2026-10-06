package com.researchassistant.ai.service;

import com.researchassistant.ai.client.AiHttpClient;
import com.researchassistant.common.exception.AiServiceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AiService {

    private static final Logger log =
            LoggerFactory.getLogger(AiService.class);

    private final AiHttpClient aiHttpClient;
    private final AiAccessService aiAccessService;

    public AiService(
            AiHttpClient aiHttpClient,
            AiAccessService aiAccessService) {

        this.aiHttpClient = aiHttpClient;
        this.aiAccessService = aiAccessService;
    }

    // =========================================================
    // ASK QUESTION
    // =========================================================

    public String askQuestion(
            String question,
            Long projectId,
            String documentId,
            String email) {

        aiAccessService.validateAccess(
                projectId,
                documentId,
                email
        );

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("question", question);
        body.put("project_id", projectId);

        if (documentId != null && !documentId.isBlank()) {
            body.put("document_id", documentId);
        }

        JsonNode response =
                aiHttpClient.postJson("/api/ai/ask", body);

        if (response == null || !response.hasNonNull("answer")) {
            throw new AiServiceException(
                    "Answer not found in AI service response");
        }

        String answer = response.get("answer").asString();

        log.info("Received answer for project {} ({} chars)",
                projectId, answer.length());

        return answer;
    }

    // =========================================================
    // INGEST PAPER
    // =========================================================

    /**
     * Sends a PDF to the Python service for chunking and embedding.
     *
     * @param fileName the stored (UUID) file name; never the
     *                 user-supplied original file name
     */
    public void ingestPaper(
            byte[] fileBytes,
            String fileName,
            String documentId,
            Long projectId) {

        String boundary = "----ResearchPaperBoundary" + UUID.randomUUID();

        ByteArrayOutputStream body = new ByteArrayOutputStream();

        writeFormField(body, boundary, "document_id", documentId);
        writeFormField(body, boundary, "project_id", String.valueOf(projectId));

        writeAscii(body, "--" + boundary + "\r\n");
        writeAscii(body, "Content-Disposition: form-data; name=\"file\"; filename=\""
                + fileName + "\"\r\n");
        writeAscii(body, "Content-Type: application/pdf\r\n\r\n");
        body.writeBytes(fileBytes);
        writeAscii(body, "\r\n--" + boundary + "--\r\n");

        log.info("Sending PDF {} ({} bytes) to AI service",
                fileName, fileBytes.length);

        aiHttpClient.postMultipart(
                "/api/ai/ingest",
                body.toByteArray(),
                boundary
        );
    }

    // =========================================================
    // DELETE VECTORS
    // =========================================================

    public void deleteProjectVectors(Long projectId) {

        aiHttpClient.delete("/api/ai/projects/" + projectId);
    }

    public void deleteDocumentVectors(
            String documentId,
            Long projectId) {

        aiHttpClient.delete(
                "/api/ai/documents/"
                        + URLEncoder.encode(documentId, StandardCharsets.UTF_8)
                        + "?project_id=" + projectId
        );
    }

    // =========================================================
    // MULTIPART HELPERS
    // =========================================================

    private void writeFormField(
            ByteArrayOutputStream body,
            String boundary,
            String name,
            String value) {

        writeAscii(body, "--" + boundary + "\r\n");
        writeAscii(body, "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
        body.writeBytes((value + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private void writeAscii(ByteArrayOutputStream body, String text) {
        body.writeBytes(text.getBytes(StandardCharsets.UTF_8));
    }
}
