package com.aistudy.backend.material;

import com.aistudy.backend.analytics.ActivityEventService;
import com.aistudy.backend.project.Project;
import com.aistudy.backend.project.ProjectService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialUploadValidationTest {

    private static final long HUNDRED_MB = 100L * 1024 * 1024;

    @Mock
    private MaterialRepository materials;
    @Mock
    private ProjectService projects;
    @Mock
    private StorageService storage;
    @Mock
    private MaterialProcessor processor;
    @Mock
    private ActivityEventService events;
    @Mock
    private MultipartFile file;

    private MaterialService service;
    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        service = new MaterialService(materials, projects, storage, processor, events);
    }

    @AfterEach
    void cleanup() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private void pdfFile(long size) throws Exception {
        org.mockito.Mockito.lenient().when(file.isEmpty()).thenReturn(size == 0);
        org.mockito.Mockito.lenient().when(file.getSize()).thenReturn(size);
        org.mockito.Mockito.lenient().when(file.getOriginalFilename()).thenReturn("notes.pdf");
        org.mockito.Mockito.lenient().when(file.getContentType()).thenReturn("application/pdf");
        org.mockito.Mockito.lenient().when(file.getInputStream())
                .thenReturn(new ByteArrayInputStream(new byte[0]));
    }

    @Test
    void rejectsFileLargerThan100MB() throws Exception {
        pdfFile(HUNDRED_MB + 1);
        when(projects.requireOwned(userId, projectId)).thenReturn(new Project());

        assertThatThrownBy(() -> service.upload(userId, projectId, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("max 100MB");
        verify(materials, never()).save(any());
    }

    @Test
    void acceptsFileOfExactly100MB() throws Exception {
        pdfFile(HUNDRED_MB);
        when(projects.requireOwned(userId, projectId)).thenReturn(new Project());
        when(materials.save(any(Material.class))).thenAnswer(inv -> {
            Material m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });
        when(storage.store(any(), any(), any(), anyLong())).thenReturn("path");
        TransactionSynchronizationManager.initSynchronization();

        MaterialDto.MaterialResponse out = service.upload(userId, projectId, file);

        assertThat(out.fileSize()).isEqualTo(HUNDRED_MB);
        assertThat(out.status()).isEqualTo("QUEUED");
    }

    @Test
    void stillRejectsNonPdf() throws Exception {
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getOriginalFilename()).thenReturn("notes.txt");
        when(file.getContentType()).thenReturn("text/plain");
        when(projects.requireOwned(userId, projectId)).thenReturn(new Project());

        assertThatThrownBy(() -> service.upload(userId, projectId, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only PDF");
        verify(materials, never()).save(any());
    }

    @Test
    void stillRejectsEmptyUpload() throws Exception {
        when(file.isEmpty()).thenReturn(true);
        when(projects.requireOwned(userId, projectId)).thenReturn(new Project());

        assertThatThrownBy(() -> service.upload(userId, projectId, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No file");
        verify(materials, never()).save(any());
    }
}
