package com.researchassistant.project.controller;

import com.researchassistant.project.dto.CreateProjectRequest;
import com.researchassistant.project.dto.ProjectResponse;
import com.researchassistant.project.service.ProjectService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            Authentication authentication) {

        String email = authentication.getName();

        ProjectResponse response =
                projectService.createProject(request, email);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getMyProjects(
            Authentication authentication) {

        String email = authentication.getName();

        return ResponseEntity.ok(
                projectService.getMyProjects(email)
        );
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProject(
            @PathVariable Long projectId,
            Authentication authentication) {

        String email = authentication.getName();

        return ResponseEntity.ok(
                projectService.getProject(projectId, email)
        );
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long projectId,
            Authentication authentication) {

        String email = authentication.getName();

        projectService.deleteProject(projectId, email);

        return ResponseEntity.noContent().build();
    }
}
