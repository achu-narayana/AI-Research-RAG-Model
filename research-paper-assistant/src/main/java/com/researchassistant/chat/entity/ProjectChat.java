package com.researchassistant.chat.entity;

import com.researchassistant.project.entity.Project;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "project_chats",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "project_id")
    }
)
public class ProjectChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "project_id",
        nullable = false,
        unique = true
    )
    private Project project;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public ProjectChat() {
    }

    public ProjectChat(Project project, LocalDateTime createdAt) {
        this.project = project;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}