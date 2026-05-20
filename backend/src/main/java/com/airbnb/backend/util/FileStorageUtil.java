package com.airbnb.backend.util;

import com.airbnb.backend.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;

@Component
@Slf4j
public class FileStorageUtil {

    @Value("${app.upload.dir}")
    private String uploadDir;


    public String saveFile(MultipartFile file, String subDirectory) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String newFilename = UUID.randomUUID().toString() + extension;

        Path directoryPath = Paths.get(uploadDir, subDirectory);

        try {
            Files.createDirectories(directoryPath);

            Path filePath = directoryPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath,
                    StandardCopyOption.REPLACE_EXISTING);

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
            Path filePath = Paths.get(uploadDir, relativePath);
            Files.deleteIfExists(filePath);
            log.info("File deleted: {}", relativePath);
        } catch (IOException e) {
            log.warn("Could not delete file {}: {}", relativePath, e.getMessage());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File cannot be empty");
        }

        if (file.getSize() > AppConstants.MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds maximum limit of 10MB");
        }

        String contentType = file.getContentType();
        boolean isAllowed = Arrays.asList(AppConstants.ALLOWED_IMAGE_TYPES)
                .contains(contentType);

        if (!isAllowed) {
            throw new BadRequestException(
                    "Invalid file type. Only JPEG, PNG and WebP images are allowed");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg"; // default
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}