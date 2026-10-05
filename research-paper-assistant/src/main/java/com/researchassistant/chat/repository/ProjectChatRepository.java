package com.researchassistant.chat.repository;

import com.researchassistant.chat.entity.ProjectChat;
import com.researchassistant.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectChatRepository
        extends JpaRepository<ProjectChat, Long> {

    Optional<ProjectChat> findByProject(Project project);
}