package com.linkchat.interfaces.rest;

import com.linkchat.application.OwnerNotificationService;
import com.linkchat.application.owner.OwnerAccountService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class OwnerController {
    private final OwnerAccountService owners;
    private final OwnerNotificationService notifications;

    public OwnerController(OwnerAccountService owners, OwnerNotificationService notifications) {
        this.owners = owners;
        this.notifications = notifications;
    }

    public record NotificationRegistration(String installationId) {
    }

    @GetMapping
    public OwnerAccountService.OwnerView me(@AuthenticationPrincipal Jwt jwt) {
        return owners.me(jwt.getSubject());
    }

    @GetMapping("/conversations")
    public Object conversations(@AuthenticationPrincipal Jwt jwt) {
        return owners.inbox(jwt.getSubject());
    }

    @PostMapping("/notifications")
    public void registerNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody NotificationRegistration request) {
        notifications.registerOwner(jwt.getSubject(), request.installationId());
    }
}
