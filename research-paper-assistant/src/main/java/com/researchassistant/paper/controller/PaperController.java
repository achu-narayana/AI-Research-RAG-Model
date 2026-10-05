package com.researchassistant.paper.controller;

import com.researchassistant.paper.dto.PaperResponse;
import com.researchassistant.paper.service.PaperService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;
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
            Authentication authentication)
            throws IOException {

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
    
    @PostMapping("/multiple")
    public ResponseEntity<?> uploadMultiplePapers(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("projectId") Long projectId,
            Authentication authentication) throws IOException {

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
}