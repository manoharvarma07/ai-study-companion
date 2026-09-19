package com.aistudy.backend.space;

import com.aistudy.backend.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpaceServiceOwnershipTest {

    @Test
    void cannotAccessAnotherUsersSpace() {
        SpaceRepository repo = mock(SpaceRepository.class);
        UUID owner = UUID.randomUUID(), intruder = UUID.randomUUID(), spaceId = UUID.randomUUID();
        when(repo.findByIdAndUserId(spaceId, intruder)).thenReturn(Optional.empty());
        var service = new SpaceService(repo);
        assertThatThrownBy(() -> service.requireOwned(intruder, spaceId))
                .isInstanceOf(ResourceNotFoundException.class);
        // owner lookup path delegates to the same scoped finder (returns empty here too)
        assertThatThrownBy(() -> service.requireOwned(owner, spaceId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void ownedSpaceIsReturned() {
        SpaceRepository repo = mock(SpaceRepository.class);
        UUID owner = UUID.randomUUID(), spaceId = UUID.randomUUID();
        Space s = new Space();
        s.setId(spaceId);
        s.setName("S");
        when(repo.findByIdAndUserId(spaceId, owner)).thenReturn(Optional.of(s));
        assertThat(new SpaceService(repo).requireOwned(owner, spaceId).getName()).isEqualTo("S");
    }
}
