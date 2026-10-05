package com.researchassistant.ai.service;

import com.researchassistant.paper.entity.Paper;
import com.researchassistant.paper.repository.PaperRepository;
import com.researchassistant.project.entity.Project;
import com.researchassistant.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;

@Service
public class AiAccessService {

    private final ProjectRepository projectRepository;
    private final PaperRepository paperRepository;

    public AiAccessService(
            ProjectRepository projectRepository,
            PaperRepository paperRepository) {

        this.projectRepository = projectRepository;
        this.paperRepository = paperRepository;
    }

    public void validateAccess(
            Long projectId,
            String documentId,
            String email) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new RuntimeException("Project not found"));

        // Check project ownership
        if (!project.getOwner().getEmail().equals(email)) {
            throw new RuntimeException(
                    "You are not authorized to access this project");
        }

        // If a specific paper is selected,
        // check that it exists and belongs to this project
        if (documentId != null && !documentId.isBlank()) {

            Paper paper = paperRepository
                    .findByDocumentId(documentId)
                    .orElseThrow(() ->
                            new RuntimeException("Paper not found"));

            if (!paper.getProject().getId().equals(projectId)) {
                throw new RuntimeException(
                        "This paper does not belong to the selected project");
            }
        }
    }
}