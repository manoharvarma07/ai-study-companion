package com.aistudy.backend.project;

import com.aistudy.backend.analytics.ActivityEventService;
import com.aistudy.backend.common.exception.ResourceNotFoundException;
import com.aistudy.backend.space.Space;
import com.aistudy.backend.space.SpaceService;
import com.aistudy.backend.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProjectService {
    private final ProjectRepository projects;
    private final SpaceService spaces;
    private final ActivityEventService events;

    public ProjectService(ProjectRepository projects, SpaceService spaces, ActivityEventService events) {
        this.projects = projects;
        this.spaces = spaces;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<ProjectDto.ProjectResponse> list(UUID userId, UUID spaceId) {
        spaces.requireOwned(userId, spaceId); // ownership of parent space
        return projects.findBySpaceIdAndUserIdOrderByCreatedAtDesc(spaceId, userId)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public ProjectDto.ProjectResponse create(UUID userId, UUID spaceId, ProjectDto.ProjectRequest req) {
        Space space = spaces.requireOwned(userId, spaceId);
        User stub = new User();
        stub.setId(userId);
        Project p = new Project();
        p.setUser(stub);
        p.setSpace(space);
        p.setName(req.name().trim());
        p.setDescription(req.description());
        p.setGoal(req.goal());
        Project saved = projects.save(p);
        events.record(userId, saved.getId(), "PROJECT_CREATED",
                Map.of("projectId", saved.getId().toString(), "name", saved.getName()),
                "project-created-" + saved.getId());
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public ProjectDto.ProjectResponse get(UUID userId, UUID projectId) {
        return toDto(requireOwned(userId, projectId));
    }

    @Transactional
    public ProjectDto.ProjectResponse update(UUID userId, UUID projectId, ProjectDto.ProjectRequest req) {
        Project p = requireOwned(userId, projectId);
        p.setName(req.name().trim());
        p.setDescription(req.description());
        p.setGoal(req.goal());
        return toDto(projects.save(p));
    }

    @Transactional
    public void delete(UUID userId, UUID projectId) {
        projects.delete(requireOwned(userId, projectId));
    }

    /** Service-level ownership check: all project access goes through here. */
    public Project requireOwned(UUID userId, UUID projectId) {
        return projects.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    private ProjectDto.ProjectResponse toDto(Project p) {
        return new ProjectDto.ProjectResponse(
                p.getId().toString(), p.getSpace().getId().toString(),
                p.getName(), p.getDescription(), p.getGoal(),
                p.getCreatedAt() == null ? null : p.getCreatedAt().toString(),
                p.getUpdatedAt() == null ? null : p.getUpdatedAt().toString());
    }
}
