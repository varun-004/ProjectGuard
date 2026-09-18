package com.projectguard.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Handles profile photo upload, validation, and deletion.
 * Files are stored under: <upload.dir>/profile-photos/
 * The column value stored in DB is: "profile-photos/<uuid>.<ext>"
 * The URL served is:       http://host/uploads/profile-photos/<uuid>.<ext>
 */
@Service
public class ProfilePhotoService {

    private static final long MAX_FILE_BYTES = 5L * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp"
    );

    private final Path uploadRootPath;

    public ProfilePhotoService(
            @Value("${app.upload.dir:uploads}") String uploadDir) throws IOException {

        this.uploadRootPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path photoDir = this.uploadRootPath.resolve("profile-photos");
        Files.createDirectories(photoDir);
    }

    /**
     * Validates and stores the uploaded photo.
     *
     * @return relative path stored in DB, e.g. "profile-photos/abc.jpg"
     */
    public String store(MultipartFile file) throws IOException {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        String newFilename = UUID.randomUUID() + "." + extension;
        String relativePath = "profile-photos/" + newFilename;

        Path destination = uploadRootPath.resolve(relativePath).normalize();

        // Guard against path traversal
        if (!destination.startsWith(uploadRootPath)) {
            throw new IllegalArgumentException("Invalid file path");
        }

        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        return relativePath;
    }

    /**
     * Deletes the file at the given relative path (e.g. "profile-photos/abc.jpg").
     * Silently ignores if the file does not exist.
     */
    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;

        try {
            Path target = uploadRootPath.resolve(relativePath).normalize();
            // Guard against path traversal
            if (!target.startsWith(uploadRootPath)) return;
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // Best-effort deletion
        }
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file provided");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new IllegalArgumentException(
                    "File is too large. Maximum allowed size is 5 MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Invalid file type. Allowed: JPG, JPEG, PNG, WEBP");
        }

        String ext = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException(
                    "Invalid file extension. Allowed: jpg, jpeg, png, webp");
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg"; // fallback
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase().trim();
    }
}
