package com.researchassistant.project.dto;

import com.researchassistant.project.entity.Project;

import java.time.LocalDateTime;

public class ProjectResponse {

    private Long id;
    private String title;
    private String description;
    private LocalDateTime createdAt;

    public ProjectResponse(
            Long id,
            String title,
            String description,
            LocalDateTime createdAt) {

        this.id = id;
        this.title = title;
        this.description = description;
        this.createdAt = createdAt;
    }

    public static ProjectResponse fromEntity(Project project) {

        return new ProjectResponse(
                project.getId(),
                project.getTitle(),
                project.getDescription(),
                project.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}