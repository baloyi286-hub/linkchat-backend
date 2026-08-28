package com.linkchat.interfaces.rest;

import com.linkchat.application.invite.CreateInviteLinkUseCase;
import com.linkchat.application.invite.GetInviteUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invites")
public class InviteController {
    private final CreateInviteLinkUseCase createInviteLink;
    private final GetInviteUseCase getInvite;

    public InviteController(CreateInviteLinkUseCase createInviteLink, GetInviteUseCase getInvite) {
        this.createInviteLink = createInviteLink;
        this.getInvite = getInvite;
    }

    public record CreateInviteRequest(
            @NotBlank(message = "displayName is required")
            @Size(max = 100, message = "displayName cannot exceed 100 characters") String displayName) {}

    @PostMapping
    public ResponseEntity<CreateInviteLinkUseCase.InviteLinkView> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateInviteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(createInviteLink.create(request.displayName(), jwt.getSubject()));
    }

    @GetMapping("/{code}")
    public GetInviteUseCase.InviteView get(@PathVariable String code) { return getInvite.get(code); }
}
