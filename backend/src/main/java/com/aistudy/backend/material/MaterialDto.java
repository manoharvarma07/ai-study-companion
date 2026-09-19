package com.aistudy.backend.material;

public class MaterialDto {
    public record MaterialResponse(String id, String filename, String contentType, long fileSize,
                                   Integer pageCount, String status, int processingAttempts,
                                   String errorMessage, String createdAt, String updatedAt) {}

    public static MaterialResponse toDto(Material m) {
        return new MaterialResponse(
                m.getId().toString(), m.getFilename(), m.getContentType(), m.getFileSize(),
                m.getPageCount(), m.getStatus().name(), m.getProcessingAttempts(),
                m.getErrorMessage(),
                m.getCreatedAt() == null ? null : m.getCreatedAt().toString(),
                m.getUpdatedAt() == null ? null : m.getUpdatedAt().toString());
    }
}
