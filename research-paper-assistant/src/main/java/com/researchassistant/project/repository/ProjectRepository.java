package com.researchassistant.project.repository;

import com.researchassistant.project.entity.Project;
import com.researchassistant.user.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findAllByOwnerOrderByCreatedAtDesc(User owner);
}