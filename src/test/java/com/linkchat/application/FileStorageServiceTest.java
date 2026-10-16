package com.linkchat.application;

import com.linkchat.application.exception.BusinessRuleException;
import com.linkchat.application.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import java.sql.ResultSet;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FileStorageServiceTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final FileStorageService storage = new FileStorageService(jdbc);

    @Test
    void storesImageInDatabase() {
        var file = new MockMultipartFile("images", "avatar.jpg", "image/jpeg", new byte[]{1, 2, 3});
        String key = storage.store(file);
        assertThat(key).isNotBlank();
        verify(jdbc).update(contains("INSERT INTO image_blob"), eq(key), eq("image/jpeg"), eq(new byte[]{1, 2, 3}));
    }

    @Test
    void rejectsInvalidImageType() {
        var file = new MockMultipartFile("images", "text.txt", "text/plain", new byte[]{1});
        assertThatThrownBy(() -> storage.store(file)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsInvalidStorageKey() {
        assertThatThrownBy(() -> storage.load("../outside.jpg")).isInstanceOf(BusinessRuleException.class);
    }
}
