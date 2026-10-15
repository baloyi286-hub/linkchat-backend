package com.linkchat.application;

import com.linkchat.application.exception.BusinessRuleException;
import com.linkchat.application.exception.ResourceNotFoundException;
import com.linkchat.application.exception.StorageException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class FileStorageService {
    private final JdbcTemplate jdbc;

    public FileStorageService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessRuleException("Image file is required");
        String type = file.getContentType();
        if (type == null || !type.matches("image/(jpeg|png|webp|gif)"))
            throw new BusinessRuleException("Only JPEG, PNG, WebP and GIF images are allowed");
        String key = UUID.randomUUID().toString();
        try {
            jdbc.update("INSERT INTO image_blob (storage_key, content_type, image_data) VALUES (?, ?, ?)",
                    key, type, file.getBytes());
            return key;
        } catch (IOException | DataAccessException exception) {
            throw new StorageException("Unable to store image", exception);
        }
    }

    public StoredImage load(String key) {
        if (key == null || !key.matches("[a-fA-F0-9-]{36}"))
            throw new BusinessRuleException("Invalid image key");
        return jdbc.query("SELECT content_type, image_data FROM image_blob WHERE storage_key = ?",
                rs -> {
                    if (!rs.next()) throw new ResourceNotFoundException("Image not found");
                    return new StoredImage(rs.getString(1), rs.getBytes(2));
                }, key);
    }

    public record StoredImage(String contentType, byte[] bytes) {}
}
