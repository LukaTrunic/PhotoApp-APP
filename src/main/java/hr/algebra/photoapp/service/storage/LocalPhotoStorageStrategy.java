package hr.algebra.photoapp.service.storage;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

// Strategy Pattern (Concrete Implementation) + Singleton (Spring managed)
// Implements local filesystem storage for photos
@Service
public class LocalPhotoStorageStrategy implements PhotoStorageStrategy {

    private static final String UPLOAD_DIR = "uploads";

    @Override
    public void store(String username, String filename, byte[] data) throws IOException {
        Path userDir = Paths.get(UPLOAD_DIR, username);
        Files.createDirectories(userDir);

        Path filePath = userDir.resolve(filename);
        Files.write(filePath, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    @Override
    public byte[] load(String path) throws IOException {
        // Remove leading slash if it is there
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        Path filePath = Paths.get(cleanPath);
        
        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + path);
        }
        
        return Files.readAllBytes(filePath);
    }

    @Override
    public void delete(String path) throws IOException {
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        Path filePath = Paths.get(cleanPath);
        
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }
    }

    @Override
    public boolean exists(String path) {
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        Path filePath = Paths.get(cleanPath);
        return Files.exists(filePath);
    }
}
