package com.ecslab;

import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Dev implementation — saves photos to ./uploads/ on disk and serves them
 * at http://localhost:8080/uploads/{filename}. No AWS credentials required.
 */
@Service
@Profile("dev")
public class LocalStorageService implements StorageService {

    private static final Path UPLOAD_DIR = Paths.get("uploads");

    public LocalStorageService() throws IOException {
        Files.createDirectories(UPLOAD_DIR);
    }

    @Override
    public String upload(MultipartFile file) throws IOException {
        String filename = UUID.randomUUID() + extension(file.getOriginalFilename());
        Files.write(UPLOAD_DIR.resolve(filename), file.getBytes());
        return filename;
    }

    @Override
    public String getUrl(String key) {
        return "http://localhost:8080/uploads/" + key;
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(UPLOAD_DIR.resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete file: " + key, e);
        }
    }

    private String extension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.'));
    }

    // Serves uploaded files from disk at /uploads/{filename}
    @RestController
    @Profile("dev")
    static class UploadController {
        @GetMapping("/uploads/{filename:.+}")
        public ResponseEntity<Resource> serve(@PathVariable String filename) throws IOException {
            Path file = UPLOAD_DIR.resolve(filename).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists()) return ResponseEntity.notFound().build();
            String contentType = Files.probeContentType(file);
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                .body(resource);
        }
    }
}
