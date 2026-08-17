package com.linkchat.application;

import com.linkchat.application.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void storeWritesFileInsideConfiguredDirectory() throws Exception {
        FileStorageService storage = new FileStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("images", "avatar.jpg", "image/jpeg", new byte[]{1, 2, 3});

        String key = storage.store(file);

        Path stored = storage.resolve(key);
        assertThat(stored).startsWith(tempDir);
        assertThat(Files.exists(stored)).isTrue();
        assertThat(Files.readAllBytes(stored)).containsExactly(1, 2, 3);
    }

    @Test
    void resolveRejectsPathTraversal() {
        FileStorageService storage = new FileStorageService(tempDir.toString());

        assertThatThrownBy(() -> storage.resolve("../outside.jpg"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Invalid image path");
    }
}
