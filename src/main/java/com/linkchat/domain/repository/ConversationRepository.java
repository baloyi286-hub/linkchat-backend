package com.linkchat.domain.repository;
import com.linkchat.domain.model.Conversation; import java.util.*;
public interface ConversationRepository {
    Conversation save(Conversation c);
    Optional<Conversation> findById(UUID id);
    List<Conversation> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
    void deleteById(UUID id);
}
