package com.aistudy.backend.space;

import com.aistudy.backend.common.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/spaces")
public class SpaceController {
    private final SpaceService service;

    public SpaceController(SpaceService service) {
        this.service = service;
    }

    @GetMapping
    public List<SpaceDto.SpaceResponse> list() {
        return service.list(CurrentUser.id());
    }

    @PostMapping
    public SpaceDto.SpaceResponse create(@Valid @RequestBody SpaceDto.SpaceRequest req) {
        return service.create(CurrentUser.id(), req);
    }

    @GetMapping("/{spaceId}")
    public SpaceDto.SpaceResponse get(@PathVariable UUID spaceId) {
        return service.get(CurrentUser.id(), spaceId);
    }

    @PutMapping("/{spaceId}")
    public SpaceDto.SpaceResponse update(@PathVariable UUID spaceId,
                                         @Valid @RequestBody SpaceDto.SpaceRequest req) {
        return service.update(CurrentUser.id(), spaceId, req);
    }

    @DeleteMapping("/{spaceId}")
    public ResponseEntity<Void> delete(@PathVariable UUID spaceId) {
        service.delete(CurrentUser.id(), spaceId);
        return ResponseEntity.noContent().build();
    }
}
