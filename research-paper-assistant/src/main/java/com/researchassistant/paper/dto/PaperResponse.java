package com.researchassistant.paper.dto;

import com.researchassistant.paper.entity.Paper;
import java.time.LocalDateTime;

public class PaperResponse {

    private Long id;
    private String documentId;
    private String originalFileName;
    private String status;
    private LocalDateTime uploadedAt;

    public PaperResponse() {
    }

    public PaperResponse(
            Long id,
            String documentId,
            String originalFileName,
            String status,
            LocalDateTime uploadedAt) {

        this.id = id;
        this.documentId = documentId;
        this.originalFileName = originalFileName;
        this.status = status;
        this.uploadedAt = uploadedAt;
    }

    public static PaperResponse fromEntity(Paper paper) {
        return new PaperResponse(
                paper.getId(),
                paper.getDocumentId(),
                paper.getOriginalFileName(),
                paper.getStatus(),
                paper.getUploadedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
}