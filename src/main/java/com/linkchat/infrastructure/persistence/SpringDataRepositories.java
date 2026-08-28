package com.linkchat.infrastructure.persistence;

import com.linkchat.domain.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

interface AccountJpaRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByInviteCode(String inviteCode);
    Optional<Account> findByAuthSubject(String authSubject);
}

interface VisitorJpaRepository extends JpaRepository<VisitorProfile, UUID> {
    Optional<VisitorProfile> findByBrowserTokenHash(String hash);
}

interface VisitorImageJpaRepository extends JpaRepository<VisitorImage, UUID> {
    List<VisitorImage> findByVisitorId(UUID visitorId);
    void deleteByVisitorId(UUID visitorId);
}

interface ConversationJpaRepository extends JpaRepository<Conversation, UUID> {
    List<Conversation> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
}

interface ChatMessageJpaRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);
}
