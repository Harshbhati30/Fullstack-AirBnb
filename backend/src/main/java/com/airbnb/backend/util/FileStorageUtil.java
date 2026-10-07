package com.airbnb.backend.util;

import com.airbnb.backend.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class FileStorageUtil {

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/jpg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    @Value("${app.upload.dir}")
    private String uploadDir;

    public String saveFile(MultipartFile file, String subDirectory) {
        validateImage(file);

        String extension = EXTENSIONS.get(file.getContentType());
        String newFilename = UUID.randomUUID() + extension;

        Path directoryPath = Paths.get(uploadDir, subDirectory);

        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(directoryPath);
            Files.copy(in, directoryPath.resolve(newFilename), StandardCopyOption.REPLACE_EXISTING);

            String relativePath = subDirectory + "/" + newFilename;
            log.info("File saved successfully: {}", relativePath);
            return relativePath;

        } catch (IOException e) {
            log.error("Failed to save file: {}", e.getMessage());
            throw new BadRequestException("Failed to save image. Please try again.");
        }
    }

    public void deleteFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;

        try {
            Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = root.resolve(relativePath).normalize();


            if (!filePath.startsWith(root)) {
                log.warn("Refusing to delete file outside upload dir: {}", relativePath);
                return;
            }
            Files.deleteIfExists(filePath);
            log.info("File deleted: {}", relativePath);
        } catch (IOException e) {
            log.warn("Could not delete file {}: {}", relativePath, e.getMessage());
        }
    }


    public void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File cannot be empty");
        }
        if (file.getSize() > AppConstants.MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds maximum limit of 10MB");
        }
        if (!EXTENSIONS.containsKey(file.getContentType())) {
            throw new BadRequestException(
                    "Invalid file type. Only JPEG, PNG and WebP images are allowed");
        }
        if (!hasImageSignature(file)) {
            throw new BadRequestException("File content is not a valid image");
        }
    }


    private boolean hasImageSignature(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            byte[] h = in.readNBytes(12);
            if (h.length < 12) return false;
            boolean jpeg = (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
            boolean png = (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G';
            boolean webp = h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                    && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
            return jpeg || png || webp;
        } catch (IOException e) {
            return false;
        }
    }
}