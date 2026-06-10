package hr.algebra.photoapp.service.storage;

import java.io.IOException;

// Strategy Pattern
// Defines interface for different storage implementations (local, cloud, etc.)
// This allows easy switching between storage backends without changing business logic
public interface PhotoStorageStrategy {

    // Store photo bytes to storage
    void store(String username, String filename, byte[] data) throws IOException;
    
    // Load photo bytes from storage
    byte[] load(String path) throws IOException;
    
    // Delete photo from storage
    void delete(String path) throws IOException;
    
    // Check if photo exists in storage
    boolean exists(String path);
}
