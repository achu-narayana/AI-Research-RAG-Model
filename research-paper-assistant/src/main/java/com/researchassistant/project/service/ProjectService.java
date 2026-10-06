package com.researchassistant.project.service;

import com.researchassistant.ai.service.AiService;
import com.researchassistant.chat.repository.ChatMessageRepository;
import com.researchassistant.chat.repository.ProjectChatRepository;
import com.researchassistant.common.exception.NotFoundException;
import com.researchassistant.paper.entity.Paper;
import com.researchassistant.paper.repository.PaperRepository;
import com.researchassistant.paper.service.PaperService;
import com.researchassistant.project.dto.CreateProjectRequest;
import com.researchassistant.project.dto.ProjectResponse;
import com.researchassistant.project.entity.Project;
import com.researchassistant.project.repository.ProjectRepository;
import com.researchassistant.user.entity.User;
import com.researchassistant.user.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectAccessService projectAccessService;
    private final PaperRepository paperRepository;
    private final PaperService paperService;
    private final ProjectChatRepository projectChatRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AiService aiService;
    private final TransactionTemplate transactionTemplate;

    public ProjectService(
            ProjectRepository projectRepository,
            UserRepository userRepository,
            ProjectAccessService projectAccessService,
            PaperRepository paperRepository,
            PaperService paperService,
            ProjectChatRepository projectChatRepository,
            ChatMessageRepository chatMessageRepository,
            AiService aiService,
            PlatformTransactionManager transactionManager) {

        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.projectAccessService = projectAccessService;
        this.paperRepository = paperRepository;
        this.paperService = paperService;
        this.projectChatRepository = projectChatRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.aiService = aiService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public ProjectResponse createProject(
            CreateProjectRequest request,
            String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new NotFoundException("User not found"));

        Project project = new Project();

        project.setTitle(request.getTitle().trim());
        project.setDescription(request.getDescription());
        project.setCreatedAt(LocalDateTime.now());
        project.setOwner(user);

        Project savedProject = projectRepository.save(project);

        return ProjectResponse.fromEntity(savedProject);
    }

    public List<ProjectResponse> getMyProjects(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new NotFoundException("User not found"));

        return projectRepository
                .findAllByOwnerOrderByCreatedAtDesc(user)
                .stream()
                .map(ProjectResponse::fromEntity)
                .toList();
    }

    public ProjectResponse getProject(Long projectId, String email) {

        return ProjectResponse.fromEntity(
                projectAccessService.getOwnedProject(projectId, email)
        );
    }

    /**
     * Deletes a project with its chat, messages, papers and files.
     *
     * The Python vectors are removed FIRST. If that call fails an
     * AiServiceException (502/503/504) is thrown and nothing is
     * deleted, so the database and the vector store stay consistent.
     */
    public void deleteProject(Long projectId, String email) {

        Project project =
                projectAccessService.getOwnedProject(projectId, email);

        // 1. Remove vectors in Python (no DB transaction open)
        aiService.deleteProjectVectors(projectId);

        // 2. Delete DB rows in a single transaction
        List<Paper> papers = transactionTemplate.execute(status -> {

            projectChatRepository.findByProject(project)
                    .ifPresent(chat -> {
                        chatMessageRepository.deleteAllByChat(chat);
                        projectChatRepository.delete(chat);
                    });

            List<Paper> projectPapers =
                    paperRepository.findAllByProjectOrderByUploadedAtDesc(project);

            paperRepository.deleteAll(projectPapers);

            projectRepository.deleteById(projectId);

            return projectPapers;
        });

        // 3. Delete files after the commit
        if (papers != null) {
            papers.forEach(paperService::deleteStoredFile);
        }
    }
}
