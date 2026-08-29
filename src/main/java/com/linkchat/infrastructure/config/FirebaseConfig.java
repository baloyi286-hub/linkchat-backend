package com.linkchat.infrastructure.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    @Bean
    @ConditionalOnProperty(name = "app.firebase.service-account-json")
    FirebaseMessaging firebaseMessaging(
            @Value("${app.firebase.service-account-json}") String serviceAccountJson,
            @Value("${app.firebase.project-id:}") String projectId) throws Exception {

        GoogleCredentials credentials = GoogleCredentials.fromStream(
                new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8)));

        FirebaseOptions.Builder options = FirebaseOptions.builder()
                .setCredentials(credentials);

        if (projectId != null && !projectId.isBlank()) {
            options.setProjectId(projectId);
        }

        FirebaseApp app = FirebaseApp.getApps().stream()
                .findFirst()
                .orElseGet(() -> FirebaseApp.initializeApp(options.build()));

        return FirebaseMessaging.getInstance(app);
    }
}
