package com.aistudy.backend.project;

import com.aistudy.backend.common.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class ProjectController {
    private final ProjectService service;

    public ProjectController(ProjectService service) {
        this.service = service;
    }

    @GetMapping("/api/spaces/{spaceId}/projects")
    public List<ProjectDto.ProjectResponse> list(@PathVariable UUID spaceId) {
        return service.list(CurrentUser.id(), spaceId);
    }

    @PostMapping("/api/spaces/{spaceId}/projects")
    public ProjectDto.ProjectResponse create(@PathVariable UUID spaceId,
                                             @Valid @RequestBody ProjectDto.ProjectRequest req) {
        return service.create(CurrentUser.id(), spaceId, req);
    }

    @GetMapping("/api/projects/{projectId}")
    public ProjectDto.ProjectResponse get(@PathVariable UUID projectId) {
        return service.get(CurrentUser.id(), projectId);
    }

    @PutMapping("/api/projects/{projectId}")
    public ProjectDto.ProjectResponse update(@PathVariable UUID projectId,
                                             @Valid @RequestBody ProjectDto.ProjectRequest req) {
        return service.update(CurrentUser.id(), projectId, req);
    }

    @DeleteMapping("/api/projects/{projectId}")
    public ResponseEntity<Void> delete(@PathVariable UUID projectId) {
        service.delete(CurrentUser.id(), projectId);
        return ResponseEntity.noContent().build();
    }
}
