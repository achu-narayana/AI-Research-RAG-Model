package com.researchassistant.ai.service;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class SummaryService {

    private final HttpClient httpClient;
    private final AiAccessService aiAccessService;

    public SummaryService(
            AiAccessService aiAccessService) {

        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        this.aiAccessService = aiAccessService;
    }

    public String generateSummary(
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

        try {

            // -----------------------------------------
            // 2. Build JSON manually
            // -----------------------------------------

            StringBuilder json = new StringBuilder();

            json.append("{")
                .append("\"project_id\":")
                .append(projectId);

            // document_id is optional
            if (documentId != null && !documentId.isBlank()) {

                json.append(",")
                    .append("\"document_id\":\"")
                    .append(escapeJson(documentId))
                    .append("\"");
            }

            json.append("}");

            String jsonBody = json.toString();

            System.out.println(
                    "Sending summary request to Python:"
            );

            System.out.println(
                    "JSON body: " + jsonBody
            );

            // -----------------------------------------
            // 3. Create HTTP request
            // -----------------------------------------

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "http://127.0.0.1:8000/api/ai/summary"
                    ))
                    .version(HttpClient.Version.HTTP_1_1)
                    .header(
                            "Content-Type",
                            "application/json"
                    )
                    .header(
                            "Accept",
                            "application/json"
                    )
                    .POST(
                            HttpRequest.BodyPublishers.ofString(
                                    jsonBody
                            )
                    )
                    .build();

            // -----------------------------------------
            // 4. Send request
            // -----------------------------------------

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            // -----------------------------------------
            // 5. Print Python response
            // -----------------------------------------

            System.out.println(
                    "Python summary status: "
                            + response.statusCode()
            );

            System.out.println(
                    "Python summary response: "
                            + response.body()
            );

            // -----------------------------------------
            // 6. Check response
            // -----------------------------------------

            if (response.statusCode() != 200) {

                throw new RuntimeException(
                        "Python AI summary failed. Status: "
                                + response.statusCode()
                                + " Response: "
                                + response.body()
                );
            }

            return response.body();

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Failed to communicate with AI summary service",
                    e
            );
        }
    }

    // -----------------------------------------
    // JSON escaping
    // -----------------------------------------

    private String escapeJson(String value) {

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}