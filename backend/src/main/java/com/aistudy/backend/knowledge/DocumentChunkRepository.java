package com.aistudy.backend.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    List<DocumentChunk> findByProjectIdAndUserId(UUID projectId, UUID userId);

    long countByProjectIdAndUserId(UUID projectId, UUID userId);

    void deleteByMaterialIdAndUserId(UUID materialId, UUID userId);

    /**
     * Project-scoped semantic search. NEVER called without a project filter,
     * so one project's material can never leak into another project.
     */
    @Query(value = """
            SELECT c.id AS id, c.content AS content, c.page_number AS pageNumber,
                   m.filename AS materialName,
                   (c.embedding <=> CAST(:queryVector AS vector)) AS distance
            FROM document_chunks c
            JOIN materials m ON m.id = c.material_id
            WHERE c.project_id = :projectId
              AND c.user_id = :userId
              AND c.embedding IS NOT NULL
            ORDER BY c.embedding <=> CAST(:queryVector AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<ChunkHit> searchByProject(@Param("projectId") UUID projectId,
                                  @Param("userId") UUID userId,
                                  @Param("queryVector") String queryVector,
                                  @Param("limit") int limit);

    interface ChunkHit {
        UUID getId();
        String getContent();
        Integer getPageNumber();
        String getMaterialName();
        Double getDistance();
    }
}
