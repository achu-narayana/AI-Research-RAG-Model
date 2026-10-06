package com.researchassistant.paper.controller;

import com.researchassistant.paper.dto.PaperResponse;
import com.researchassistant.paper.service.PaperService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class PaperController {

    private final PaperService paperService;

    public PaperController(PaperService paperService) {
        this.paperService = paperService;
    }

    @PostMapping(
            value = "/{projectId}/papers",
            consumes = "multipart/form-data"
    )
    public ResponseEntity<PaperResponse> uploadPaper(
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        String email = authentication.getName();

        PaperResponse response =
                paperService.uploadPaper(
                        projectId,
                        file,
                        email
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Bulk upload. Returns 200 with all papers when everything
     * succeeds. If some files fail, the successful ones are kept and
     * a 502 {"message": "Uploaded X of Y papers. Failed: ..."} is
     * returned.
     */
    @PostMapping("/multiple")
    public ResponseEntity<List<PaperResponse>> uploadMultiplePapers(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("projectId") Long projectId,
            Authentication authentication) {

        String email = authentication.getName();

        List<PaperResponse> papers =
                paperService.uploadMultiplePapers(
                        projectId,
                        files,
                        email
                );

        return ResponseEntity.ok(papers);
    }

    @GetMapping("/{projectId}/papers")
    public ResponseEntity<List<PaperResponse>> getProjectPapers(
            @PathVariable Long projectId,
            Authentication authentication) {

        String email = authentication.getName();

        return ResponseEntity.ok(
                paperService.getProjectPapers(
                        projectId,
                        email
                )
        );
    }

    @DeleteMapping("/{projectId}/papers/{documentId}")
    public ResponseEntity<Void> deletePaper(
            @PathVariable Long projectId,
            @PathVariable String documentId,
            Authentication authentication) {

        String email = authentication.getName();

        paperService.deletePaper(
                projectId,
                documentId,
                email
        );

        return ResponseEntity.noContent().build();
    }
}
