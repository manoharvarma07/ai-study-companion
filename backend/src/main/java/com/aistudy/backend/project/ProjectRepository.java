package com.aistudy.backend.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findBySpaceIdAndUserIdOrderByCreatedAtDesc(UUID spaceId, UUID userId);
    Optional<Project> findByIdAndUserId(UUID id, UUID userId);
    Optional<Project> findByIdAndSpaceIdAndUserId(UUID id, UUID spaceId, UUID userId);
    long countByUserId(UUID userId);
}
