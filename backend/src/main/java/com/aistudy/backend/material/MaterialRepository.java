package com.aistudy.backend.material;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MaterialRepository extends JpaRepository<Material, UUID> {
    List<Material> findByProjectIdAndUserIdOrderByCreatedAtDesc(UUID projectId, UUID userId);
    Optional<Material> findByIdAndUserId(UUID id, UUID userId);
    Optional<Material> findByIdAndProjectIdAndUserId(UUID id, UUID projectId, UUID userId);
    long countByProjectIdAndUserId(UUID projectId, UUID userId);
    long countByProjectIdAndUserIdAndStatus(UUID projectId, UUID userId, Material.MaterialStatus status);
    long countByUserId(UUID userId);
    List<Material> findTop10ByUserIdOrderByCreatedAtDesc(UUID userId);
}
