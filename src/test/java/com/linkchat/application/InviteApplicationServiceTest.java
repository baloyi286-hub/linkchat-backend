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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InviteApplicationServiceTest {

    @Mock
    AccountRepository accounts;

    @Mock
    InviteCodeGenerator codeGenerator;

    private InviteApplicationService service;

    @BeforeEach
    void setUp() {
        service = new InviteApplicationService(accounts, codeGenerator);
    }

    @Test
    void createsShareableInviteLink() {
        when(codeGenerator.generate()).thenReturn("AbC234xYz789");
        when(accounts.findByInviteCode("AbC234xYz789")).thenReturn(Optional.empty());
        when(accounts.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create("  Grant  ");

        assertThat(result.displayName()).isEqualTo("Grant");
        assertThat(result.inviteCode()).isEqualTo("AbC234xYz789");
        assertThat(result.invitePath()).isEqualTo("/i/AbC234xYz789");
        assertThat(result.ownerId()).isNotNull();
    }

    @Test
    void retriesWhenGeneratedCodeAlreadyExists() {
        Account existing =
        Account.create("Existing", "ABC123");
        when(codeGenerator.generate()).thenReturn("duplicate", "freshCode123");
        when(accounts.findByInviteCode("duplicate"))
                .thenReturn(Optional.of(existing));
        when(accounts.findByInviteCode("freshCode123")).thenReturn(Optional.empty());
        when(accounts.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create("Owner");

        assertThat(result.inviteCode()).isEqualTo("freshCode123");
    }

    @Test
    void rejectsBlankDisplayName() {
        assertThatThrownBy(() -> service.create("   "))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Display name is required");
    }
}
