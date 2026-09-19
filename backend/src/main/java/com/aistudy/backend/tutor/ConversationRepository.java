package com.aistudy.backend.tutor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    List<Conversation> findByProjectIdAndUserIdOrderByUpdatedAtDesc(UUID projectId, UUID userId);
    Optional<Conversation> findByIdAndUserId(UUID id, UUID userId);
    Optional<Conversation> findByIdAndProjectIdAndUserId(UUID id, UUID projectId, UUID userId);
    long countByUserId(UUID userId);
    List<Conversation> findTop10ByUserIdOrderByUpdatedAtDesc(UUID userId);
}
