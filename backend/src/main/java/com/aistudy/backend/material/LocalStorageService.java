package com.aistudy.backend.material;

import com.aistudy.backend.common.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {
    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);
    private final Path root;

    public LocalStorageService(AppProperties props) throws IOException {
        this.root = Paths.get(props.storage().location()).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    @Override
    public String store(UUID materialId, String filename, InputStream data, long size) throws IOException {
        String safe = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path dir = root.resolve(materialId.toString());
        Files.createDirectories(dir);
        Path target = dir.resolve(safe);
        Files.copy(data, target, StandardCopyOption.REPLACE_EXISTING);
        log.info("Stored upload {} ({} bytes) at {}", filename, size, target);
        return target.toString();
    }

    @Override
    public InputStream load(String storagePath) throws IOException {
        Path p = Paths.get(storagePath);
        if (!p.normalize().startsWith(root)) {
            throw new SecurityException("Invalid storage path");
        }
        return Files.newInputStream(p);
    }

    @Override
    public void delete(String storagePath) {
        try {
            if (storagePath != null) {
                Path p = Paths.get(storagePath);
                if (p.normalize().startsWith(root)) {
                    Files.deleteIfExists(p);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to delete stored file {}: {}", storagePath, e.getMessage());
        }
    }
}
