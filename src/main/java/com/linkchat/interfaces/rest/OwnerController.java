package com.linkchat.interfaces.rest;

import com.linkchat.application.owner.OwnerAccountService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class OwnerController {
    private final OwnerAccountService owners;

    public OwnerController(OwnerAccountService owners) { this.owners = owners; }

    @GetMapping
    public OwnerAccountService.OwnerView me(@AuthenticationPrincipal Jwt jwt) {
        return owners.me(jwt.getSubject());
    }

    @GetMapping("/conversations")
    public Object conversations(@AuthenticationPrincipal Jwt jwt) {
        return owners.inbox(jwt.getSubject());
    }
}
