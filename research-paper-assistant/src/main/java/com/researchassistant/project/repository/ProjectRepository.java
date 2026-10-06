package com.researchassistant.project.repository;

import com.researchassistant.project.entity.Project;
import com.researchassistant.user.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findAllByOwnerOrderByCreatedAtDesc(User owner);

    @Query("select p from Project p join fetch p.owner where p.id = :id")
    Optional<Project> findWithOwnerById(@Param("id") Long id);
}
