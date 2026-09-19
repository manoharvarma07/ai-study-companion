package com.aistudy.backend.material;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Storage abstraction. The prototype uses local filesystem storage;
 * an S3/Supabase implementation can be added later behind this interface.
 */
public interface StorageService {
    /** Stores the upload and returns an opaque storage path/key. */
    String store(UUID materialId, String filename, InputStream data, long size) throws IOException;

    /** Opens a stream for a previously stored object. */
    InputStream load(String storagePath) throws IOException;

    /** Deletes a stored object if it exists. */
    void delete(String storagePath);
}
