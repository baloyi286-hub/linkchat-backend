package com.linkchat.interfaces.rest;

import com.linkchat.application.ChatApplicationService;
import com.linkchat.application.FileStorageService;
import com.linkchat.application.OwnerNotificationService;
import com.linkchat.application.exception.BusinessRuleException;
import com.linkchat.application.exception.ResourceNotFoundException;
import com.linkchat.domain.model.SenderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatApplicationService app;
    private final FileStorageService storage;
    private final OwnerNotificationService notifications;

    public ChatController(
            ChatApplicationService app,
            FileStorageService storage,
            OwnerNotificationService notifications) {
        this.app = app;
        this.storage = storage;
        this.notifications = notifications;
    }

    public record TokenRequest(@NotBlank(message = "browserToken is required") String browserToken) {
    }

    public record StartRequest(@NotBlank(message = "browserToken is required") String browserToken) {
    }

    public record SendRequest(
            @NotBlank(message = "senderType is required") String senderType,
            @NotBlank(message = "body is required") String body) {
    }

    public record VisitorNotificationRegistration(
            @NotBlank(message = "browserToken is required") String browserToken,
            @NotBlank(message = "installationId is required") String installationId) {
    }

    @PostMapping("/visitors/lookup")
    public Object lookup(@Valid @RequestBody TokenRequest request) {
        return app.lookupVisitor(request.browserToken())
                .<Object>map(visitor -> Map.of("found", true, "visitor", visitor))
                .orElse(Map.of("found", false));
    }

    @PostMapping(value = "/visitors/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Object profile(
            @RequestPart String browserToken,
            @RequestPart String displayName,
            @RequestPart(required = false) List<MultipartFile> images,
            @RequestParam(defaultValue = "false") boolean replaceImages) {
        return app.upsertVisitor(browserToken, displayName, images, replaceImages);
    }

    @PostMapping("/visitors/notifications")
    public void registerVisitorNotifications(@Valid @RequestBody VisitorNotificationRegistration request) {
        notifications.registerVisitor(request.browserToken(), request.installationId());
    }

    @PostMapping("/invites/{code}/conversations")
    public Object start(@PathVariable String code, @Valid @RequestBody StartRequest request) {
        return app.startConversation(code, request.browserToken());
    }

    @GetMapping("/conversations/{id}/messages")
    public Object history(@PathVariable UUID id) {
        return app.history(id);
    }

    @PostMapping("/conversations/{id}/messages")
    public Object send(@PathVariable UUID id, @Valid @RequestBody SendRequest request) {
        return app.send(id, parseSenderType(request.senderType()), request.body());
    }

    @GetMapping("/owners/{inviteCode}/conversations")
    public Object inbox(@PathVariable String inviteCode) {
        return app.inbox(inviteCode);
    }

    @GetMapping("/images/{key}")
    public ResponseEntity<Resource> image(@PathVariable String key) {
        Path path = storage.resolve(key);
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("Image not found");
        }
        log.debug("Serving image. key={}", key);
        return ResponseEntity.ok()
                .contentType(MediaTypeFactory.getMediaType(path.getFileName().toString())
                        .orElse(MediaType.APPLICATION_OCTET_STREAM))
                .body(new FileSystemResource(path));
    }

    private SenderType parseSenderType(String value) {
        try {
            return SenderType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException("senderType must be OWNER or VISITOR");
        }
    }
}
