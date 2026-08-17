package com.linkchat.interfaces.rest;

import com.linkchat.application.invite.CreateInviteLinkUseCase;
import com.linkchat.application.invite.GetInviteUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invites")
public class InviteController {

    private final CreateInviteLinkUseCase createInviteLink;
    private final GetInviteUseCase getInvite;

    public InviteController(
            CreateInviteLinkUseCase createInviteLink,
            GetInviteUseCase getInvite) {
        this.createInviteLink = createInviteLink;
        this.getInvite = getInvite;
    }

    public record CreateInviteRequest(
            @NotBlank(message = "displayName is required")
            @Size(max = 100, message = "displayName cannot exceed 100 characters")
            String displayName) {
    }

    @PostMapping
    public ResponseEntity<CreateInviteLinkUseCase.InviteLinkView> create(
            @Valid @RequestBody CreateInviteRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createInviteLink.create(request.displayName()));
    }

    @GetMapping("/{code}")
    public GetInviteUseCase.InviteView get(@PathVariable String code) {
        return getInvite.get(code);
    }
}
