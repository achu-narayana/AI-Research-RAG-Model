package com.researchassistant.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CompareRequest {

    @NotNull(message = "Project ID is required")
    private Long projectId;

    @NotBlank(message = "First document ID is required")
    private String documentId1;

    @NotBlank(message = "Second document ID is required")
    private String documentId2;

    public CompareRequest() {
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getDocumentId1() {
        return documentId1;
    }

    public void setDocumentId1(String documentId1) {
        this.documentId1 = documentId1;
    }

    public String getDocumentId2() {
        return documentId2;
    }

    public void setDocumentId2(String documentId2) {
        this.documentId2 = documentId2;
    }
}