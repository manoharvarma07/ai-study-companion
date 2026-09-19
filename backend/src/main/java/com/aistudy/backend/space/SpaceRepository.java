package com.aistudy.backend.space;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpaceRepository extends JpaRepository<Space, UUID> {
    List<Space> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<Space> findByIdAndUserId(UUID id, UUID userId);
    long countByUserId(UUID userId);
}
