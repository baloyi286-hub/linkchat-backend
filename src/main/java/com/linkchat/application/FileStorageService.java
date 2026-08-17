package com.linkchat.application;

import com.linkchat.application.exception.BusinessRuleException;
import com.linkchat.application.exception.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private final Path root;

    public FileStorageService(@Value("${app.upload-dir}") String directory) {
        try {
            this.root = Paths.get(directory).toAbsolutePath().normalize();
            Files.createDirectories(root);
            log.info("Upload storage initialized. directory={}", root);
        } catch (IOException exception) {
            throw new StorageException("Unable to initialize image storage", exception);
        }
    }

    public String store(MultipartFile file) {
        try {
            String extension = safeExtension(file.getOriginalFilename());
            String key = UUID.randomUUID() + extension;
            Path target = root.resolve(key).normalize();
            ensureInsideUploadDirectory(target);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Image stored. storageKey={} size={} contentType={}", key, file.getSize(), file.getContentType());
            return key;
        } catch (IOException exception) {
            log.error("Image storage failed. originalName={}", file.getOriginalFilename(), exception);
            throw new StorageException("Unable to store image", exception);
        }
    }

    public Path resolve(String key) {
        if (key == null || key.isBlank()) {
            throw new BusinessRuleException("Image key is required");
        }
        Path path = root.resolve(key).normalize();
        ensureInsideUploadDirectory(path);
        return path;
    }

    private String safeExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        return extension.matches("\\.[A-Za-z0-9]{1,10}") ? extension.toLowerCase() : "";
    }

    private void ensureInsideUploadDirectory(Path path) {
        if (!path.startsWith(root)) {
            throw new BusinessRuleException("Invalid image path");
        }
    }
}
