package com.aistudy.backend.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ActivityEventServiceTest {

    @Test
    void duplicateIdempotencyKeyIsIgnored() {
        ActivityEventRepository repo = mock(ActivityEventRepository.class);
        when(repo.findByIdempotencyKey("k-1")).thenReturn(Optional.of(new ActivityEvent()));
        var service = new ActivityEventService(repo, new ObjectMapper());
        service.record(UUID.randomUUID(), UUID.randomUUID(), "QUIZ_STARTED", Map.of(), "k-1");
        verify(repo, never()).save(any());
    }

    @Test
    void freshEventIsRecorded() {
        ActivityEventRepository repo = mock(ActivityEventRepository.class);
        when(repo.findByIdempotencyKey("k-2")).thenReturn(Optional.empty());
        var service = new ActivityEventService(repo, new ObjectMapper());
        UUID user = UUID.randomUUID(), project = UUID.randomUUID();
        service.record(user, project, "TUTOR_MESSAGE", Map.of("a", "b"), "k-2");
        ArgumentCaptor<ActivityEvent> captor = ArgumentCaptor.forClass(ActivityEvent.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("TUTOR_MESSAGE");
        assertThat(captor.getValue().getIdempotencyKey()).isEqualTo("k-2");
    }
}
