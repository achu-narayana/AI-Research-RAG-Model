package com.researchassistant.paper.service;

import com.researchassistant.ai.service.AiService;
import com.researchassistant.common.exception.AiServiceException;
import com.researchassistant.common.exception.ApiException;
import com.researchassistant.common.exception.BadRequestException;
import com.researchassistant.common.exception.NotFoundException;
import com.researchassistant.paper.dto.PaperResponse;
import com.researchassistant.paper.entity.Paper;
import com.researchassistant.paper.repository.PaperRepository;
import com.researchassistant.project.entity.Project;
import com.researchassistant.project.service.ProjectAccessService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Note: methods here are deliberately NOT @Transactional, so no
 * database transaction is held open while Python ingests a PDF.
 */
@Service
public class PaperService {

    private static final Logger log =
            LoggerFactory.getLogger(PaperService.class);

    private static final int MAX_FILES_PER_UPLOAD = 5;

    private static final byte[] PDF_MAGIC =
            "%PDF".getBytes(StandardCharsets.US_ASCII);

    private final PaperRepository paperRepository;
    private final ProjectAccessService projectAccessService;
    private final AiService aiService;
    private final Path uploadDirectory;

    public PaperService(
            PaperRepository paperRepository,
            ProjectAccessService projectAccessService,
            AiService aiService,
            @Value("${app.upload-dir:uploads/papers}") String uploadDir) {

        this.paperRepository = paperRepository;
        this.projectAccessService = projectAccessService;
        this.aiService = aiService;
        this.uploadDirectory = Paths.get(uploadDir);
    }

    // =========================================================
    // SINGLE UPLOAD
    // =========================================================

    public PaperResponse uploadPaper(
            Long projectId,
            MultipartFile file,
            String email) {

        // 1. Project exists and belongs to the user
        Project project =
                projectAccessService.getOwnedProject(projectId, email);

        // 2. Validate file
        String error = validatePdf(file);

        if (error != null) {
            throw new BadRequestException(error);
        }

        // 3. Store, ingest, mark READY
        return processUpload(project, file);
    }

    // =========================================================
    // BULK UPLOAD
    // =========================================================

    /**
     * Validates every file first (nothing is stored if any file is
     * invalid), then processes them one by one.
     *
     * Successful uploads are kept even if others fail. If any file
     * fails during processing, an AiServiceException (502) is thrown
     * whose message names the failed files; the client should
     * refresh the paper list to see the successful ones.
     */
    public List<PaperResponse> uploadMultiplePapers(
            Long projectId,
            List<MultipartFile> files,
            String email) {

        if (files == null || files.isEmpty()) {
            throw new BadRequestException("At least one PDF is required");
        }

        if (files.size() > MAX_FILES_PER_UPLOAD) {
            throw new BadRequestException(
                    "You can upload a maximum of "
                            + MAX_FILES_PER_UPLOAD + " PDFs at a time");
        }

        Project project =
                projectAccessService.getOwnedProject(projectId, email);

        // ----------------------------------------------
        // 1. Validate all files first
        // ----------------------------------------------

        List<String> invalid = new ArrayList<>();

        for (MultipartFile file : files) {

            String error = validatePdf(file);

            if (error != null) {
                invalid.add(displayName(file) + " (" + error + ")");
            }
        }

        if (!invalid.isEmpty()) {
            throw new BadRequestException(
                    "Invalid files: " + String.join("; ", invalid));
        }

        // ----------------------------------------------
        // 2. Process each file
        // ----------------------------------------------

        List<PaperResponse> uploadedPapers = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        for (MultipartFile file : files) {

            try {
                uploadedPapers.add(processUpload(project, file));

            } catch (RuntimeException e) {
                log.warn("Bulk upload: {} failed: {}",
                        displayName(file), e.getMessage());

                failed.add(displayName(file) + " (" + e.getMessage() + ")");
            }
        }

        if (!failed.isEmpty()) {
            throw new AiServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "Uploaded " + uploadedPapers.size() + " of "
                            + files.size() + " papers. Failed: "
                            + String.join("; ", failed));
        }

        return uploadedPapers;
    }

    // =========================================================
    // LIST PAPERS
    // =========================================================

    public List<PaperResponse> getProjectPapers(
            Long projectId,
            String email) {

        Project project =
                projectAccessService.getOwnedProject(projectId, email);

        return paperRepository
                .findAllByProjectOrderByUploadedAtDesc(project)
                .stream()
                .map(PaperResponse::fromEntity)
                .toList();
    }

    // =========================================================
    // DELETE PAPER
    // =========================================================

    public void deletePaper(
            Long projectId,
            String documentId,
            String email) {

        projectAccessService.getOwnedProject(projectId, email);

        Paper paper = paperRepository
                .findByDocumentIdAndProjectId(documentId, projectId)
                .orElseThrow(() ->
                        new NotFoundException("Paper not found in this project"));

        // Remove vectors first; if Python fails, nothing is deleted
        aiService.deleteDocumentVectors(documentId, projectId);

        paperRepository.delete(paper);

        deleteStoredFile(paper);
    }

    /**
     * Deletes the stored PDF of a paper from disk. Failures are
     * logged but not rethrown.
     */
    public void deleteStoredFile(Paper paper) {

        if (paper.getFilePath() == null) {
            return;
        }

        try {
            Files.deleteIfExists(Paths.get(paper.getFilePath()));

        } catch (IOException | InvalidPathException e) {
            log.warn("Could not delete file {}: {}",
                    paper.getFilePath(), e.getMessage());
        }
    }

    // =========================================================
    // INTERNALS
    // =========================================================

    private PaperResponse processUpload(
            Project project,
            MultipartFile file) {

        String originalFileName = file.getOriginalFilename();
        String storedFileName = UUID.randomUUID() + ".pdf";
        Path filePath = uploadDirectory.resolve(storedFileName);

        byte[] bytes;

        // 1. Save PDF to disk
        try {
            bytes = file.getBytes();

            Files.createDirectories(uploadDirectory);
            Files.write(filePath, bytes);

        } catch (IOException e) {
            log.error("Failed to store uploaded file", e);
            throw new IllegalStateException("Failed to store uploaded file", e);
        }

        // 2. Save metadata as PROCESSING
        Paper paper = new Paper();

        paper.setDocumentId(UUID.randomUUID().toString());
        paper.setOriginalFileName(originalFileName);
        paper.setStoredFileName(storedFileName);
        paper.setFilePath(filePath.toString());
        paper.setStatus("PROCESSING");
        paper.setUploadedAt(LocalDateTime.now());
        paper.setProject(project);

        Paper savedPaper;

        try {
            savedPaper = paperRepository.save(paper);

        } catch (RuntimeException e) {
            deleteStoredFile(paper);
            throw e;
        }

        // 3. Send PDF to Python AI service (no transaction open)
        try {
            aiService.ingestPaper(
                    bytes,
                    storedFileName,
                    savedPaper.getDocumentId(),
                    project.getId()
            );

        } catch (RuntimeException e) {

            // Roll back: remove row and file
            paperRepository.delete(savedPaper);
            deleteStoredFile(savedPaper);

            if (e instanceof AiServiceException aiError) {
                throw new AiServiceException(
                        aiError.getStatus(),
                        "Failed to process " + originalFileName
                                + ": " + aiError.getMessage(),
                        aiError);
            }

            if (e instanceof ApiException) {
                throw e;
            }

            throw new AiServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to process " + originalFileName,
                    e);
        }

        // 4. Mark READY
        savedPaper.setStatus("READY");
        savedPaper = paperRepository.save(savedPaper);

        log.info("Paper {} processed ({} bytes)",
                savedPaper.getDocumentId(), bytes.length);

        return PaperResponse.fromEntity(savedPaper);
    }

    /**
     * Returns an error message, or null if the file is a valid PDF.
     */
    private String validatePdf(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            return "File cannot be empty";
        }

        String originalFileName = file.getOriginalFilename();

        if (originalFileName == null ||
                !originalFileName.toLowerCase().endsWith(".pdf")) {

            return "Only PDF files are allowed";
        }

        try (InputStream in = file.getInputStream()) {

            byte[] header = in.readNBytes(PDF_MAGIC.length);

            if (!Arrays.equals(header, PDF_MAGIC)) {
                return "File is not a valid PDF";
            }

        } catch (IOException e) {
            return "Could not read file";
        }

        return null;
    }

    private String displayName(MultipartFile file) {

        String name = file == null ? null : file.getOriginalFilename();

        return name == null || name.isBlank() ? "(unnamed file)" : name;
    }
}
