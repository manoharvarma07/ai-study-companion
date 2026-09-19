package com.aistudy.backend.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProjectDto {
    public record ProjectRequest(
            @NotBlank @Size(min = 1, max = 200) String name,
            @Size(max = 5000) String description,
            @Size(max = 2000) String goal) {}

    public record ProjectResponse(String id, String spaceId, String name,
                                  String description, String goal,
                                  String createdAt, String updatedAt) {}
}
