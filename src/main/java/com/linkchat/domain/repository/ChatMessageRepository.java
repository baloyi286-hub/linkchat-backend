package com.linkchat.domain.repository;
import com.linkchat.domain.model.ChatMessage; import java.util.*;
public interface ChatMessageRepository {
    ChatMessage save(ChatMessage m);
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);
    void deleteByConversationId(UUID conversationId);
}
