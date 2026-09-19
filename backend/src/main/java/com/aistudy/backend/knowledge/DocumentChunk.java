package com.aistudy.backend.knowledge;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_chunks")
@Getter
@Setter
@NoArgsConstructor
public class DocumentChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "material_id", nullable = false)
    private UUID materialId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    /**
     * pgvector literal like "[0.1,0.2,...]". The write cast lets Hibernate
     * persist text into the vector column; the read cast returns text.
     */
    @Column(columnDefinition = "vector(1536)")
    @ColumnTransformer(read = "embedding::text", write = "?::vector")
    private String embedding;

    @Column(name = "chunk_metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() { createdAt = Instant.now(); }
}
