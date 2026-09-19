package com.aistudy.backend.knowledge;

import com.aistudy.backend.common.security.CurrentUser;
import com.aistudy.backend.project.ProjectService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class ConceptController {
    private final ConceptService service;
    private final ProjectService projects;

    public ConceptController(ConceptService service, ProjectService projects) {
        this.service = service;
        this.projects = projects;
    }

    @GetMapping("/api/projects/{projectId}/concepts")
    public List<ConceptDto> list(@PathVariable UUID projectId) {
        UUID userId = CurrentUser.id();
        projects.requireOwned(userId, projectId);
        return service.list(projectId, userId).stream()
                .map(c -> new ConceptDto(c.getId().toString(), c.getName(), c.getDescription()))
                .toList();
    }

    public record ConceptDto(String id, String name, String description) {}
}
