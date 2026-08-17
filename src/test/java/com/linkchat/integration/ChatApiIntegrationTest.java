package com.linkchat.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class ChatApiIntegrationTest {

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("linkchat_test")
            .withUsername("linkchat")
            .withPassword("linkchat");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.upload-dir", () -> "target/test-uploads");
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void sameVisitorCanCreateTwoIndependentConversationsFromSameInvite() throws Exception {
        String token = "integration-browser-token";

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/visitors/profile")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "browserToken", "", "text/plain", token.getBytes()))
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "displayName", "", "text/plain", "Integration Visitor".getBytes()))
                        .param("replaceImages", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Integration Visitor"));

        String requestBody = objectMapper.writeValueAsString(new BrowserTokenRequest(token));

        String firstBody = mockMvc.perform(post("/api/invites/demo/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String secondBody = mockMvc.perform(post("/api/invites/demo/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode first = objectMapper.readTree(firstBody);
        JsonNode second = objectMapper.readTree(secondBody);

        assertThat(first.get("conversationId").asText())
                .isNotEqualTo(second.get("conversationId").asText());
        assertThat(second.at("/visitor/displayName").asText()).isEqualTo("Integration Visitor");
    }

    @Test
    void unknownInviteReturnsStructured404() throws Exception {
        mockMvc.perform(post("/api/invites/not-real/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BrowserTokenRequest("some-token"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/invites/not-real/conversations"));
    }

    @Test
    void blankBrowserTokenReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/visitors/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"browserToken\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.validationErrors.browserToken").value("browserToken is required"));
    }

    private record BrowserTokenRequest(String browserToken) {
    }
}
