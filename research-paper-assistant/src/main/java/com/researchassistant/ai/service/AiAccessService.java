package com.researchassistant.ai.service;

import com.researchassistant.common.exception.NotFoundException;
import com.researchassistant.paper.repository.PaperRepository;
import com.researchassistant.project.service.ProjectAccessService;
import org.springframework.stereotype.Service;

@Service
public class AiAccessService {

    private final ProjectAccessService projectAccessService;
    private final PaperRepository paperRepository;

    public AiAccessService(
            ProjectAccessService projectAccessService,
            PaperRepository paperRepository) {

        this.projectAccessService = projectAccessService;
        this.paperRepository = paperRepository;
    }

    public void validateAccess(
            Long projectId,
            String documentId,
            String email) {

        // Project exists and belongs to the user (404 / 403)
        projectAccessService.getOwnedProject(projectId, email);

        // If a specific paper is selected,
        // check that it exists in this project
        if (documentId != null && !documentId.isBlank()) {

            paperRepository
                    .findByDocumentIdAndProjectId(documentId, projectId)
                    .orElseThrow(() ->
                            new NotFoundException(
                                    "Paper not found in this project"));
        }
    }
}
