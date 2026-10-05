package com.researchassistant.ai.dto;

import jakarta.validation.constraints.NotNull;

public class SummaryRequest {

    @NotNull(message = "Project ID is required")
    private Long projectId;

    private String documentId;

    public SummaryRequest() {
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