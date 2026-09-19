package com.aistudy.backend.material;

import com.aistudy.backend.project.Project;
import com.aistudy.backend.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "materials")
@Getter
@Setter
@NoArgsConstructor
public class Material {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 500)
    private String filename;

    @Column(name = "content_type", length = 150)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "storage_path", length = 1000)
    private String storagePath;

    @Column(name = "page_count")
    private Integer pageCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MaterialStatus status = MaterialStatus.QUEUED;

    @Column(name = "processing_attempts", nullable = false)
    private int processingAttempts = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() { createdAt = Instant.now(); updatedAt = Instant.now(); }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public enum MaterialStatus { QUEUED, PROCESSING, READY, FAILED }
}
