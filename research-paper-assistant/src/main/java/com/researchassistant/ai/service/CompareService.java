package com.researchassistant.ai.service;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class CompareService {

    private final HttpClient httpClient;
    private final AiAccessService aiAccessService;

    public CompareService(AiAccessService aiAccessService) {

        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        this.aiAccessService = aiAccessService;
    }

    public String comparePapers(
            Long projectId,
            String documentId1,
            String documentId2,
            String email) {

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
        // Check that two different papers were selected
        // ----------------------------------------------

        if (documentId1.equals(documentId2)) {
            throw new RuntimeException(
                    "Please select two different research papers"
            );
        }

        try {

            // ------------------------------------------
            // Build JSON request
            // ------------------------------------------

            StringBuilder json = new StringBuilder();

            json.append("{")
                    .append("\"project_id\":")
                    .append(projectId)
                    .append(",")
                    .append("\"document_id_1\":\"")
                    .append(escapeJson(documentId1))
                    .append("\"")
                    .append(",")
                    .append("\"document_id_2\":\"")
                    .append(escapeJson(documentId2))
                    .append("\"")
                    .append("}");

            String jsonBody = json.toString();

            System.out.println("========================================");
            System.out.println("Sending comparison request to Python");
            System.out.println("JSON body: " + jsonBody);
            System.out.println("========================================");

            // ------------------------------------------
            // Send request to FastAPI
            // ------------------------------------------

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "http://127.0.0.1:8000/api/ai/compare"
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
                            HttpRequest.BodyPublishers
                                    .ofString(jsonBody)
                    )
                    .build();

            // ------------------------------------------
            // Get response
            // ------------------------------------------

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            System.out.println(
                    "Python comparison status: "
                            + response.statusCode()
            );

            System.out.println(
                    "Python comparison response: "
                            + response.body()
            );

            // ------------------------------------------
            // Check response
            // ------------------------------------------

            if (response.statusCode() != 200) {

                throw new RuntimeException(
                        "Python AI comparison failed. Status: "
                                + response.statusCode()
                                + " Response: "
                                + response.body()
                );
            }

            return response.body();

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Failed to communicate with AI comparison service",
                    e
            );
        }
    }

    // ----------------------------------------------
    // JSON escaping
    // ----------------------------------------------

    private String escapeJson(String value) {

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\\n")
                .replace("\r", "\\\r")
                .replace("\t", "\\\t");
    }
}