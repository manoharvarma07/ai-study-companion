package com.aistudy.backend.tutor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findByConversationIdAndUserIdOrderByCreatedAtAsc(UUID conversationId, UUID userId);
    List<Message> findTop10ByConversationIdAndUserIdOrderByCreatedAtDesc(UUID conversationId, UUID userId);
    long countByProjectIdAndUserId(UUID projectId, UUID userId);
    long countByUserId(UUID userId);
}
