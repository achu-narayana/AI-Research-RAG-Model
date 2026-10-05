package com.researchassistant.ai.service;

import org.springframework.stereotype.Service;
import com.researchassistant.project.entity.Project;
import com.researchassistant.project.repository.ProjectRepository;
import com.researchassistant.paper.entity.Paper;
import com.researchassistant.paper.repository.PaperRepository;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
@Service
public class AiService {

    private final HttpClient httpClient;
    private final ProjectRepository projectRepository;
    private final PaperRepository paperRepository;
    private final AiAccessService aiAccessService;

    public AiService(ProjectRepository projectRepository,
            PaperRepository paperRepository,
            AiAccessService aiAccessService) {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.projectRepository = projectRepository;
        this.paperRepository = paperRepository;
        this.aiAccessService = aiAccessService;
    }

    public String askQuestion(
            String question,
            Long projectId,
            String documentId,
            String email
    )  {
    	aiAccessService.validateAccess(
                projectId,
                documentId,
                email
        );
    	Project project = projectRepository.findById(projectId)
    	        .orElseThrow(() ->
    	                new RuntimeException("Project not found"));

    	if (!project.getOwner().getEmail().equals(email)) {
    	    throw new RuntimeException(
    	            "You are not authorized to access this project");
    	}
    	if (documentId != null && !documentId.isBlank()) {

    	    Paper paper = paperRepository.findByDocumentId(documentId)
    	            .orElseThrow(() ->
    	                    new RuntimeException("Paper not found"));

    	    if (!paper.getProject().getId().equals(projectId)) {
    	        throw new RuntimeException(
    	                "This paper does not belong to the selected project");
    	    }
    	}

        try {

            StringBuilder json = new StringBuilder();

            json.append("{")
                .append("\"question\":\"")
                .append(escapeJson(question))
                .append("\",")
                .append("\"project_id\":")
                .append(projectId);

            if (documentId != null && !documentId.isBlank()) {
                json.append(",")
                    .append("\"document_id\":\"")
                    .append(escapeJson(documentId))
                    .append("\"");
            }

            json.append("}");

            String jsonBody = json.toString();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "http://127.0.0.1:8000/api/ai/ask"))
                    .header("Content-Type", "application/json")
                    .POST(
                            HttpRequest.BodyPublishers.ofString(jsonBody)
                    )
                    .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            System.out.println(
                    "Python ask status: " + response.statusCode());

            System.out.println(
                    "Python ask response: " + response.body());

            if (response.statusCode() != 200) {
                throw new RuntimeException(
                        "AI service returned status "
                        + response.statusCode()
                        + ": "
                        + response.body()
                );
            }

            String responseBody = response.body();

            String answerKey = "\"answer\":\"";

            int start = responseBody.indexOf(answerKey);

            if (start == -1) {
                throw new RuntimeException(
                        "Answer not found in AI service response"
                );
            }

            start += answerKey.length();

            int end = responseBody.lastIndexOf("\"");

            if (end <= start) {
                throw new RuntimeException(
                        "Invalid AI service response"
                );
            }

            String answer = responseBody.substring(start, end);

            // Basic JSON unescaping
            answer = answer
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");

            return answer;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to communicate with AI service",
                    e
            );
        }
    }
    private String escapeJson(String value) {

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
    public String ingestPaper(
            byte[] fileBytes,
            String fileName,
            String documentId,
            Long projectId) {

        try {

            String boundary = "----ResearchPaperBoundary" + System.currentTimeMillis();

            ByteArrayOutputStream body = new ByteArrayOutputStream();
            body.write(("--" + boundary + "\r\n")
                    .getBytes(StandardCharsets.UTF_8));

            body.write(("Content-Disposition: form-data; name=\"document_id\"\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));

            body.write((documentId + "\r\n")
                    .getBytes(StandardCharsets.UTF_8));

            body.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(("Content-Disposition: form-data; name=\"project_id\"\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            body.write((projectId + "\r\n").getBytes(StandardCharsets.UTF_8));

            body.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(("Content-Disposition: form-data; name=\"file\"; filename=\""
                    + fileName + "\"\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(("Content-Type: application/pdf\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));

            body.write(fileBytes);

            body.write(("\r\n--" + boundary + "--\r\n")
                    .getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:8000/api/ai/ingest"))
                    .header("Content-Type",
                            "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                    .build();

            System.out.println("Sending PDF to Python: " + fileName);

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            System.out.println("Python ingest status: "
                    + response.statusCode());

            System.out.println("Python ingest response: "
                    + response.body());

            if (response.statusCode() != 200) {
                throw new RuntimeException(
                        "Python AI ingestion failed: "
                        + response.statusCode()
                        + ": "
                        + response.body()
                );
            }

            return response.body();

        } catch (IOException | InterruptedException e) {

            throw new RuntimeException(
                    "Failed to send PDF to AI service",
                    e
            );
        }
    }
}