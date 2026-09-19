package com.aistudy.backend.material;

import com.aistudy.backend.common.security.CurrentUser;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
public class MaterialController {
    private final MaterialService service;
    private final MaterialProcessor processor;

    public MaterialController(MaterialService service, MaterialProcessor processor) {
        this.service = service;
        this.processor = processor;
    }

    @GetMapping("/api/projects/{projectId}/materials")
    public List<MaterialDto.MaterialResponse> list(@PathVariable UUID projectId) {
        return service.list(CurrentUser.id(), projectId);
    }

    @PostMapping(value = "/api/projects/{projectId}/materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MaterialDto.MaterialResponse upload(@PathVariable UUID projectId,
                                               @RequestParam("file") MultipartFile file) {
        return service.upload(CurrentUser.id(), projectId, file);
    }

    @GetMapping("/api/projects/{projectId}/materials/{materialId}")
    public MaterialDto.MaterialResponse get(@PathVariable UUID projectId,
                                            @PathVariable UUID materialId) {
        return service.get(CurrentUser.id(), projectId, materialId);
    }

    /** Manual retry for FAILED materials (respects max attempts + idempotency guard). */
    @PostMapping("/api/projects/{projectId}/materials/{materialId}/retry")
    public ResponseEntity<MaterialDto.MaterialResponse> retry(@PathVariable UUID projectId,
                                                             @PathVariable UUID materialId) {
        MaterialDto.MaterialResponse current = service.get(CurrentUser.id(), projectId, materialId);
        if (!"FAILED".equals(current.status())) {
            return ResponseEntity.badRequest().build();
        }
        processor.processAsync(materialId);
        return ResponseEntity.accepted().body(current);
    }
}
