package com.researchassistant.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AskQuestionRequest {

    @NotBlank(message = "Question is required")
    private String question;

    @NotNull(message = "Project ID is required")
    private Long projectId;

    // Optional
    // null = search across all papers in the project
    private String documentId;

    public AskQuestionRequest() {
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
}