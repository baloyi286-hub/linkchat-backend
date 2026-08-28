package com.linkchat.application.owner;

import com.linkchat.application.ChatApplicationService;
import com.linkchat.application.exception.ResourceNotFoundException;
import com.linkchat.domain.model.Account;
import com.linkchat.domain.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OwnerAccountService {
    private final AccountRepository accounts;
    private final ChatApplicationService chat;

    public OwnerAccountService(AccountRepository accounts, ChatApplicationService chat) {
        this.accounts = accounts;
        this.chat = chat;
    }

    @Transactional(readOnly = true)
    public OwnerView me(String authSubject) {
        return toView(requireOwner(authSubject));
    }

    @Transactional(readOnly = true)
    public Object inbox(String authSubject) {
        return chat.inbox(requireOwner(authSubject).getInviteCode());
    }

    private Account requireOwner(String subject) {
        return accounts.findByAuthSubject(subject)
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not created yet"));
    }

    private OwnerView toView(Account account) {
        return new OwnerView(account.getId(), account.getDisplayName(), account.getInviteCode(), "/i/" + account.getInviteCode());
    }

    public record OwnerView(java.util.UUID ownerId, String displayName, String inviteCode, String invitePath) {}
}
