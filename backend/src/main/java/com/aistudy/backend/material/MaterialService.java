package com.aistudy.backend.material;

import com.aistudy.backend.analytics.ActivityEventService;
import com.aistudy.backend.common.exception.ResourceNotFoundException;
import com.aistudy.backend.project.Project;
import com.aistudy.backend.project.ProjectService;
import com.aistudy.backend.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MaterialService {
    private static final Logger log = LoggerFactory.getLogger(MaterialService.class);
    private static final long MAX_BYTES = 100 * 1024 * 1024;

    private final MaterialRepository materials;
    private final ProjectService projects;
    private final StorageService storage;
    private final MaterialProcessor processor;
    private final ActivityEventService events;

    public MaterialService(MaterialRepository materials, ProjectService projects,
                           StorageService storage, MaterialProcessor processor,
                           ActivityEventService events) {
        this.materials = materials;
        this.projects = projects;
        this.storage = storage;
        this.processor = processor;
        this.events = events;
    }

    public List<MaterialDto.MaterialResponse> list(UUID userId, UUID projectId) {
        projects.requireOwned(userId, projectId);
        return materials.findByProjectIdAndUserIdOrderByCreatedAtDesc(projectId, userId)
                .stream().map(MaterialDto::toDto).toList();
    }

    /**
     * Stores the upload and returns QUEUED immediately.
     * Heavy processing happens asynchronously in {@link MaterialProcessor}.
     */
    @Transactional
    public MaterialDto.MaterialResponse upload(UUID userId, UUID projectId, MultipartFile file) {
        Project project = projects.requireOwned(userId, projectId);
        validate(file);

        Material m = new Material();
        User stub = new User();
        stub.setId(userId);
        m.setUser(stub);
        m.setProject(project);
        m.setFilename(file.getOriginalFilename() == null ? "upload.pdf" : file.getOriginalFilename());
        m.setContentType(file.getContentType());
        m.setFileSize(file.getSize());
        m.setStatus(Material.MaterialStatus.QUEUED);
        Material saved = materials.save(m);

        try {
            String path = storage.store(saved.getId(), saved.getFilename(), file.getInputStream(), file.getSize());
            saved.setStoragePath(path);
            materials.save(saved);
        } catch (Exception e) {
            log.error("Failed to store upload {}", saved.getId(), e);
            saved.setStatus(Material.MaterialStatus.FAILED);
            saved.setErrorMessage("Storage failed: " + e.getMessage());
            materials.save(saved);
            throw new IllegalStateException("Failed to store uploaded file");
        }

        events.record(userId, projectId, "MATERIAL_UPLOADED",
                Map.of("materialId", saved.getId().toString(), "filename", saved.getFilename()),
                "material-uploaded-" + saved.getId());
        // Trigger async processing only AFTER this transaction commits,
        // otherwise the worker cannot see the new row yet.
        UUID newId = saved.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                processor.processAsync(newId); // never blocks the request
            }
        });
        return MaterialDto.toDto(saved);
    }

    @Transactional(readOnly = true)
    public MaterialDto.MaterialResponse get(UUID userId, UUID projectId, UUID materialId) {
        projects.requireOwned(userId, projectId);
        Material m = materials.findByIdAndProjectIdAndUserId(materialId, projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found"));
        return MaterialDto.toDto(m);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file uploaded");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("File too large (max 100MB)");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        String ct = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!name.endsWith(".pdf") && !ct.equals("application/pdf")) {
            throw new IllegalArgumentException("Only PDF files are supported");
        }
    }
}
