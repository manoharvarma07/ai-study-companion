package com.aistudy.backend.space;

import com.aistudy.backend.common.exception.ResourceNotFoundException;
import com.aistudy.backend.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SpaceService {
    private final SpaceRepository spaces;

    public SpaceService(SpaceRepository spaces) {
        this.spaces = spaces;
    }

    public List<SpaceDto.SpaceResponse> list(UUID userId) {
        return spaces.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toDto).toList();
    }

    @Transactional
    public SpaceDto.SpaceResponse create(UUID userId, SpaceDto.SpaceRequest req) {
        User stub = new User();
        stub.setId(userId);
        Space s = new Space();
        s.setUser(stub);
        s.setName(req.name().trim());
        s.setDescription(req.description());
        return toDto(spaces.save(s));
    }

    @Transactional(readOnly = true)
    public SpaceDto.SpaceResponse get(UUID userId, UUID spaceId) {
        return toDto(requireOwned(userId, spaceId));
    }

    @Transactional
    public SpaceDto.SpaceResponse update(UUID userId, UUID spaceId, SpaceDto.SpaceRequest req) {
        Space s = requireOwned(userId, spaceId);
        s.setName(req.name().trim());
        s.setDescription(req.description());
        return toDto(spaces.save(s));
    }

    @Transactional
    public void delete(UUID userId, UUID spaceId) {
        spaces.delete(requireOwned(userId, spaceId));
    }

    /** Ownership check at service level: never expose another user's space. */
    public Space requireOwned(UUID userId, UUID spaceId) {
        return spaces.findByIdAndUserId(spaceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found"));
    }

    private SpaceDto.SpaceResponse toDto(Space s) {
        return new SpaceDto.SpaceResponse(s.getId().toString(), s.getName(), s.getDescription(),
                s.getCreatedAt() == null ? null : s.getCreatedAt().toString(),
                s.getUpdatedAt() == null ? null : s.getUpdatedAt().toString());
    }
}
