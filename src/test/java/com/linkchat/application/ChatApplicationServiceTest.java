package com.linkchat.application;

import com.linkchat.application.exception.BusinessRuleException;
import com.linkchat.application.exception.ResourceNotFoundException;
import com.linkchat.domain.model.Account;
import com.linkchat.domain.model.Conversation;
import com.linkchat.domain.model.SenderType;
import com.linkchat.domain.model.VisitorProfile;
import com.linkchat.domain.repository.AccountRepository;
import com.linkchat.domain.repository.ChatMessageRepository;
import com.linkchat.domain.repository.ConversationRepository;
import com.linkchat.domain.repository.VisitorImageRepository;
import com.linkchat.domain.repository.VisitorProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatApplicationServiceTest {

    @Mock AccountRepository accounts;
    @Mock VisitorProfileRepository visitors;
    @Mock VisitorImageRepository images;
    @Mock ConversationRepository conversations;
    @Mock ChatMessageRepository messages;
    @Mock TokenService tokens;
    @Mock FileStorageService storage;

    private ChatApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ChatApplicationService(accounts, visitors, images, conversations, messages, tokens, storage);
    }

    @Test
    void lookupVisitorReturnsSavedProfile() {
        VisitorProfile visitor = new VisitorProfile("hash", "Nkhenso");
        when(tokens.hash("browser-token")).thenReturn("hash");
        when(visitors.findByBrowserTokenHash("hash")).thenReturn(Optional.of(visitor));
        when(images.findByVisitorId(visitor.getId())).thenReturn(List.of());

        var result = service.lookupVisitor("browser-token");

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().displayName()).isEqualTo("Nkhenso");
    }

    @Test
    void upsertVisitorRejectsMoreThanFourImagesBeforePersistence() {
        List<MultipartFile> files = List.of(
                image("1.jpg"), image("2.jpg"), image("3.jpg"), image("4.jpg"), image("5.jpg"));

        assertThatThrownBy(() -> service.upsertVisitor("token", "Visitor", files, false))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Maximum 4 images");

        verify(visitors, never()).save(any());
    }

    @Test
    void upsertVisitorRejectsNonImageFile() {
        MockMultipartFile text = new MockMultipartFile("images", "note.txt", "text/plain", "hello".getBytes());

        assertThatThrownBy(() -> service.upsertVisitor("token", "Visitor", List.of(text), false))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Only image files are allowed");
    }

    @Test
    void startConversationCreatesFreshConversationForEveryCall() {
        Account owner = org.mockito.Mockito.mock(Account.class);
        UUID ownerId = UUID.randomUUID();
        VisitorProfile visitor = new VisitorProfile("hash", "Visitor");

        when(owner.getId()).thenReturn(ownerId);
        when(owner.getDisplayName()).thenReturn("Owner");
        when(accounts.findByInviteCode("demo")).thenReturn(Optional.of(owner));
        when(tokens.hash("token")).thenReturn("hash");
        when(visitors.findByBrowserTokenHash("hash")).thenReturn(Optional.of(visitor));
        when(images.findByVisitorId(visitor.getId())).thenReturn(List.of());
        when(conversations.save(any(Conversation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var first = service.startConversation("demo", "token");
        var second = service.startConversation("demo", "token");

        assertThat(first.conversationId()).isNotEqualTo(second.conversationId());
        assertThat(first.visitor().displayName()).isEqualTo("Visitor");
        verify(conversations, org.mockito.Mockito.times(2)).save(any(Conversation.class));
    }

    @Test
    void startConversationRejectsUnknownInvite() {
        when(accounts.findByInviteCode("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.startConversation("missing", "token"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Invite link not found");
    }

    @Test
    void sendRejectsBlankMessage() {
        assertThatThrownBy(() -> service.send(UUID.randomUUID(), SenderType.VISITOR, "   "))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Message is empty");
    }

    private MockMultipartFile image(String filename) {
        return new MockMultipartFile("images", filename, "image/jpeg", new byte[]{1, 2, 3});
    }
}
