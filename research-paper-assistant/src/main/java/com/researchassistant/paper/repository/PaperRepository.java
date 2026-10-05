package com.researchassistant.paper.repository;

import com.researchassistant.paper.entity.Paper;
import com.researchassistant.project.entity.Project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaperRepository extends JpaRepository<Paper, Long> {

    List<Paper> findAllByProjectOrderByUploadedAtDesc(Project project);

    Optional<Paper> findByDocumentId(String documentId);
}