package com.researchassistant.project.service;

import com.researchassistant.project.dto.CreateProjectRequest;
import com.researchassistant.project.dto.ProjectResponse;
import com.researchassistant.project.entity.Project;
import com.researchassistant.project.repository.ProjectRepository;
import com.researchassistant.user.entity.User;
import com.researchassistant.user.repository.UserRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectService(
            ProjectRepository projectRepository,
            UserRepository userRepository) {

        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    public ProjectResponse createProject(
            CreateProjectRequest request,
            String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Project project = new Project();

        project.setTitle(request.getTitle());
        project.setDescription(request.getDescription());
        project.setCreatedAt(LocalDateTime.now());
        project.setOwner(user);

        Project savedProject = projectRepository.save(project);

        return ProjectResponse.fromEntity(savedProject);
    }

    public List<ProjectResponse> getMyProjects(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        return projectRepository
                .findAllByOwnerOrderByCreatedAtDesc(user)
                .stream()
                .map(ProjectResponse::fromEntity)
                .toList();
    }
}