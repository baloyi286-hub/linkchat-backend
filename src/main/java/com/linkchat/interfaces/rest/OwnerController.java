package com.linkchat.interfaces.rest;

import com.linkchat.application.OwnerNotificationService;
import com.linkchat.application.owner.OwnerAccountService;
import com.linkchat.application.owner.OwnerProfileService;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/me")
public class OwnerController {
    private final OwnerAccountService owners; private final OwnerNotificationService notifications; private final OwnerProfileService profiles;
    public OwnerController(OwnerAccountService owners,OwnerNotificationService notifications,OwnerProfileService profiles){this.owners=owners;this.notifications=notifications;this.profiles=profiles;}
    public record NotificationRegistration(String installationId){}
    public record PasswordRequest(String password){}

    @GetMapping("/profile") public OwnerProfileService.ProfileView profile(@AuthenticationPrincipal Jwt jwt){return profiles.profile(jwt.getSubject());}
    @PostMapping(value="/profile",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public OwnerProfileService.ProfileView updateProfile(@AuthenticationPrincipal Jwt jwt,@RequestPart BigDecimal chatPrice,@RequestPart(required=false) List<MultipartFile> images,@RequestParam(defaultValue="false") boolean replaceImages){return profiles.update(jwt.getSubject(),chatPrice,images,replaceImages);}
    @GetMapping public OwnerAccountService.OwnerView me(@AuthenticationPrincipal Jwt jwt){return owners.me(jwt.getSubject());}
    @GetMapping("/conversations") public Object conversations(@AuthenticationPrincipal Jwt jwt){return owners.inbox(jwt.getSubject());}
    @PostMapping("/notifications") public void registerNotifications(@AuthenticationPrincipal Jwt jwt,@RequestBody NotificationRegistration request){notifications.registerOwner(jwt.getSubject(),request.installationId());}
    @PostMapping("/vault/password") public void setVaultPassword(@AuthenticationPrincipal Jwt jwt,@RequestBody PasswordRequest request){owners.setVaultPassword(jwt.getSubject(),request.password());}
    @PostMapping("/vault/hidden") public Object hidden(@AuthenticationPrincipal Jwt jwt,@RequestBody PasswordRequest request){return owners.hidden(jwt.getSubject(),request.password());}
    @PostMapping("/conversations/{id}/hide") public void hide(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){owners.hide(jwt.getSubject(),id);}
    @PostMapping("/conversations/{id}/restore") public void restore(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){owners.restore(jwt.getSubject(),id);}
    @DeleteMapping("/conversations/{id}") public void delete(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){owners.delete(jwt.getSubject(),id);}
}
