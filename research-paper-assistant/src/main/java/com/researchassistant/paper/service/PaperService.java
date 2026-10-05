package com.researchassistant.paper.service;

import com.researchassistant.ai.service.AiService;
import com.researchassistant.paper.dto.PaperResponse;
import com.researchassistant.paper.entity.Paper;
import com.researchassistant.paper.repository.PaperRepository;
import com.researchassistant.project.entity.Project;
import com.researchassistant.project.repository.ProjectRepository;
import com.researchassistant.user.entity.User;
import com.researchassistant.user.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaperService {

    private final PaperRepository paperRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final AiService aiService;

    private final Path uploadDirectory =
            Paths.get("uploads/papers");

    public PaperService(
            PaperRepository paperRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository,
            AiService aiService) {

        this.paperRepository = paperRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.aiService = aiService;
    }

    public PaperResponse uploadPaper(
            Long projectId,
            MultipartFile file,
            String email) throws IOException {

        // 1. Find logged-in user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        // 2. Find project
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new RuntimeException("Project not found"));

        // 3. Make sure project belongs to logged-in user
        if (!project.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException(
                    "You do not have access to this project");
        }

        // 4. Validate file
        if (file.isEmpty()) {
            throw new RuntimeException("File cannot be empty");
        }

        String originalFileName = file.getOriginalFilename();

        if (originalFileName == null ||
                !originalFileName.toLowerCase().endsWith(".pdf")) {

            throw new RuntimeException("Only PDF files are allowed");
        }

        // 5. Create upload directory
        Files.createDirectories(uploadDirectory);

        // 6. Generate unique file name
        String storedFileName =
                UUID.randomUUID() + ".pdf";

        Path filePath =
                uploadDirectory.resolve(storedFileName);

        // 7. Save PDF
        Files.copy(
                file.getInputStream(),
                filePath,
                StandardCopyOption.REPLACE_EXISTING
        );

        // 8. Save metadata
        Paper paper = new Paper();

        paper.setDocumentId(UUID.randomUUID().toString());
        paper.setOriginalFileName(originalFileName);
        paper.setStoredFileName(storedFileName);
        paper.setFilePath(filePath.toString());
        paper.setStatus("UPLOADED");
        paper.setUploadedAt(LocalDateTime.now());
        paper.setProject(project);

        Paper savedPaper = paperRepository.save(paper);

        // 9. Send PDF to Python AI service
        System.out.println("Sending uploaded PDF to Python AI service...");

        aiService.ingestPaper(
                file.getBytes(),
                originalFileName,
                savedPaper.getDocumentId(),
                projectId
        );

        System.out.println("PDF successfully processed by Python AI service.");

        // 10. Return response
        return PaperResponse.fromEntity(savedPaper);
    }
    public List<PaperResponse> uploadMultiplePapers(
            Long projectId,
            List<MultipartFile> files,
            String email) throws IOException {

        // Maximum 5 files per upload request
        if (files == null || files.isEmpty()) {
            throw new RuntimeException("At least one PDF is required");
        }

        if (files.size() > 5) {
            throw new RuntimeException(
                    "You can upload a maximum of 5 PDFs at a time");
        }

        List<PaperResponse> uploadedPapers = new java.util.ArrayList<>();

        for (MultipartFile file : files) {

            PaperResponse response = uploadPaper(
                    projectId,
                    file,
                    email
            );

            uploadedPapers.add(response);
        }

        return uploadedPapers;
    }
    public List<PaperResponse> getProjectPapers(
            Long projectId,
            String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new RuntimeException("Project not found"));

        if (!project.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException(
                    "You do not have access to this project");
        }

        return paperRepository
                .findAllByProjectOrderByUploadedAtDesc(project)
                .stream()
                .map(PaperResponse::fromEntity)
                .toList();
    }
}