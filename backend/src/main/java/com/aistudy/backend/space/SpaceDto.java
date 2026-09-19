package com.aistudy.backend.space;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SpaceDto {
    public record SpaceRequest(
            @NotBlank @Size(min = 1, max = 200) String name,
            @Size(max = 5000) String description) {}

    public record SpaceResponse(String id, String name, String description,
                                String createdAt, String updatedAt) {}
}
