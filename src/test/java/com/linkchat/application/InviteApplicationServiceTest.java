package com.linkchat.application;

import com.linkchat.application.exception.BusinessRuleException;
import com.linkchat.application.invite.InviteApplicationService;
import com.linkchat.application.port.InviteCodeGenerator;
import com.linkchat.domain.model.Account;
import com.linkchat.domain.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InviteApplicationServiceTest {
    @Mock AccountRepository accounts;
    @Mock InviteCodeGenerator codeGenerator;
    private InviteApplicationService service;

    @BeforeEach void setUp() { service = new InviteApplicationService(accounts, codeGenerator); }

    @Test void createsShareableInviteLinkForAuthenticatedOwner() {
        when(accounts.findByAuthSubject("auth0|owner-1")).thenReturn(Optional.empty());
        when(codeGenerator.generate()).thenReturn("AbC234xYz789");
        when(accounts.findByInviteCode("AbC234xYz789")).thenReturn(Optional.empty());
        when(accounts.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));
        var result = service.create("  Grant  ", "auth0|owner-1");
        assertThat(result.displayName()).isEqualTo("Grant");
        assertThat(result.inviteCode()).isEqualTo("AbC234xYz789");
        assertThat(result.invitePath()).isEqualTo("/i/AbC234xYz789");
    }

    @Test void returnsExistingOwnerInsteadOfCreatingDuplicate() {
        Account existing = Account.create("Grant", "existing123", "auth0|owner-1");
        when(accounts.findByAuthSubject("auth0|owner-1")).thenReturn(Optional.of(existing));
        var result = service.create("Ignored", "auth0|owner-1");
        assertThat(result.inviteCode()).isEqualTo("existing123");
    }

    @Test void rejectsBlankDisplayName() {
        when(accounts.findByAuthSubject("auth0|owner-1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create("   ", "auth0|owner-1"))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Display name is required");
    }

    @Test void rejectsMissingAuthenticatedOwner() {
        assertThatThrownBy(() -> service.create("Grant", " "))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Authenticated owner is required");
    }
}
