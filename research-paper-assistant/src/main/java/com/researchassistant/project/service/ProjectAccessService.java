package com.researchassistant.project.service;

import com.researchassistant.common.exception.ForbiddenException;
import com.researchassistant.common.exception.NotFoundException;
import com.researchassistant.project.entity.Project;
import com.researchassistant.project.repository.ProjectRepository;

import org.springframework.stereotype.Service;

/**
 * Loads a project (with its owner fetched) and checks that it
 * belongs to the logged-in user. Safe to call outside a transaction.
 */
@Service
public class ProjectAccessService {

    private final ProjectRepository projectRepository;

    public ProjectAccessService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public Project getOwnedProject(Long projectId, String email) {

        Project project = projectRepository.findWithOwnerById(projectId)
                .orElseThrow(() ->
                        new NotFoundException("Project not found"));

        if (email == null ||
                !project.getOwner().getEmail().equalsIgnoreCase(email)) {

            throw new ForbiddenException(
                    "You are not authorized to access this project");
        }

        return project;
    }
}
