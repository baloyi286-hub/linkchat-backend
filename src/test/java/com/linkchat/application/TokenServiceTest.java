package com.linkchat.application;

import com.linkchat.application.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenServiceTest {

    private final TokenService tokenService = new TokenService();

    @Test
    void hashIsDeterministicAndDoesNotExposeRawToken() {
        String first = tokenService.hash("browser-secret");
        String second = tokenService.hash("browser-secret");

        assertThat(first)
                .isEqualTo(second)
                .hasSize(64)
                .doesNotContain("browser-secret");
    }

    @Test
    void hashRejectsBlankToken() {
        assertThatThrownBy(() -> tokenService.hash(" "))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Browser token is required");
    }
}
