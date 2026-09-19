package com.aistudy.backend.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class ActivityEventService {
    private static final Logger log = LoggerFactory.getLogger(ActivityEventService.class);
    private final ActivityEventRepository repo;
    private final ObjectMapper mapper;

    public ActivityEventService(ActivityEventRepository repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    /**
     * Idempotent event recording: if the same idempotencyKey is submitted twice,
     * the duplicate is safely ignored.
     */
    public void record(UUID userId, UUID projectId, String eventType,
                       Map<String, Object> metadata, String idempotencyKey) {
        if (idempotencyKey != null && repo.findByIdempotencyKey(idempotencyKey).isPresent()) {
            return;
        }
        try {
            ActivityEvent e = new ActivityEvent();
            e.setUserId(userId);
            e.setProjectId(projectId);
            e.setEventType(eventType);
            e.setMetadata(metadata == null ? null : mapper.writeValueAsString(metadata));
            e.setIdempotencyKey(idempotencyKey);
            repo.save(e);
        } catch (DataIntegrityViolationException dup) {
            log.debug("Duplicate activity event ignored: {}", idempotencyKey);
        } catch (Exception ex) {
            log.warn("Failed to record activity event {}: {}", eventType, ex.getMessage());
        }
    }

    public void record(UUID userId, UUID projectId, String eventType, Map<String, Object> metadata) {
        record(userId, projectId, eventType, metadata, null);
    }
}
