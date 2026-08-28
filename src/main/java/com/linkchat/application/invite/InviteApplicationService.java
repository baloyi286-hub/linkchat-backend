package com.linkchat.application.invite;

import com.linkchat.application.exception.BusinessRuleException;
import com.linkchat.application.exception.ResourceNotFoundException;
import com.linkchat.application.port.InviteCodeGenerator;
import com.linkchat.domain.model.Account;
import com.linkchat.domain.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InviteApplicationService implements CreateInviteLinkUseCase, GetInviteUseCase {
    private static final Logger log = LoggerFactory.getLogger(InviteApplicationService.class);
    private static final int MAX_CODE_ATTEMPTS = 10;
    private final AccountRepository accounts;
    private final InviteCodeGenerator inviteCodeGenerator;

    public InviteApplicationService(AccountRepository accounts, InviteCodeGenerator inviteCodeGenerator) {
        this.accounts = accounts;
        this.inviteCodeGenerator = inviteCodeGenerator;
    }

    @Override
    @Transactional
    public InviteLinkView create(String displayName, String authSubject) {
        if (authSubject == null || authSubject.isBlank()) throw new BusinessRuleException("Authenticated owner is required");
        var existing = accounts.findByAuthSubject(authSubject);
        if (existing.isPresent()) return toLinkView(existing.get());

        String normalizedName = validateAndNormalizeName(displayName);
        Account savedOwner = accounts.save(Account.create(normalizedName, generateUniqueInviteCode(), authSubject));
        log.info("Owner account created. ownerId={} inviteCode={}", savedOwner.getId(), savedOwner.getInviteCode());
        return toLinkView(savedOwner);
    }

    @Override
    @Transactional(readOnly = true)
    public InviteView get(String inviteCode) {
        if (inviteCode == null || inviteCode.isBlank()) throw new BusinessRuleException("Invite code is required");
        Account owner = accounts.findByInviteCode(inviteCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Invite link not found"));
        return new InviteView(owner.getId(), owner.getDisplayName(), owner.getInviteCode());
    }

    private InviteLinkView toLinkView(Account owner) {
        return new InviteLinkView(owner.getId(), owner.getDisplayName(), owner.getInviteCode(), "/i/" + owner.getInviteCode());
    }

    private String validateAndNormalizeName(String displayName) {
        if (displayName == null || displayName.isBlank()) throw new BusinessRuleException("Display name is required");
        String normalized = displayName.trim();
        if (normalized.length() > 100) throw new BusinessRuleException("Display name cannot exceed 100 characters");
        return normalized;
    }

    private String generateUniqueInviteCode() {
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String candidate = inviteCodeGenerator.generate();
            if (accounts.findByInviteCode(candidate).isEmpty()) return candidate;
        }
        throw new BusinessRuleException("Could not generate a unique invite link. Please try again.");
    }
}
