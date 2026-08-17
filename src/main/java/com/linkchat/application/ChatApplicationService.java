package com.linkchat.application;

import com.linkchat.application.exception.BusinessRuleException;
import com.linkchat.application.exception.ResourceNotFoundException;
import com.linkchat.domain.model.ChatMessage;
import com.linkchat.domain.model.Conversation;
import com.linkchat.domain.model.SenderType;
import com.linkchat.domain.model.VisitorImage;
import com.linkchat.domain.model.VisitorProfile;
import com.linkchat.domain.repository.AccountRepository;
import com.linkchat.domain.repository.ChatMessageRepository;
import com.linkchat.domain.repository.ConversationRepository;
import com.linkchat.domain.repository.VisitorImageRepository;
import com.linkchat.domain.repository.VisitorProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ChatApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ChatApplicationService.class);
    private static final int MAX_IMAGES = 4;

    private final AccountRepository accounts;
    private final VisitorProfileRepository visitors;
    private final VisitorImageRepository images;
    private final ConversationRepository conversations;
    private final ChatMessageRepository messages;
    private final TokenService tokens;
    private final FileStorageService storage;

    public ChatApplicationService(
            AccountRepository accounts,
            VisitorProfileRepository visitors,
            VisitorImageRepository images,
            ConversationRepository conversations,
            ChatMessageRepository messages,
            TokenService tokens,
            FileStorageService storage) {
        this.accounts = accounts;
        this.visitors = visitors;
        this.images = images;
        this.conversations = conversations;
        this.messages = messages;
        this.tokens = tokens;
        this.storage = storage;
    }

    public record VisitorView(UUID id, String displayName, List<String> imageUrls) {
    }

    public record StartView(UUID conversationId, VisitorView visitor, String ownerName) {
    }

    public record MessageView(UUID id, String senderType, String body, String createdAt) {
    }

    @Transactional(readOnly = true)
    public Optional<VisitorView> lookupVisitor(String browserToken) {
        requireBrowserToken(browserToken);
        String tokenHash = tokens.hash(browserToken);
        Optional<VisitorView> result = visitors.findByBrowserTokenHash(tokenHash).map(this::toVisitor);
        log.debug("Visitor lookup completed. found={}", result.isPresent());
        return result;
    }

    @Transactional
    public VisitorView upsertVisitor(
            String browserToken,
            String name,
            List<MultipartFile> files,
            boolean replaceImages) {

        requireBrowserToken(browserToken);
        validateName(name);
        validateImages(files);

        String tokenHash = tokens.hash(browserToken);
        boolean existingVisitor = visitors.findByBrowserTokenHash(tokenHash).isPresent();
        VisitorProfile visitor = visitors.findByBrowserTokenHash(tokenHash)
                .orElseGet(() -> new VisitorProfile(tokenHash, name.trim()));

        visitor.rename(name.trim());
        visitor = visitors.save(visitor);

        if (replaceImages) {
            log.debug("Replacing stored visitor images. visitorId={}", visitor.getId());
            images.deleteByVisitorId(visitor.getId());
        }

        if (files != null) {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                String key = storage.store(file);
                images.save(new VisitorImage(
                        visitor.getId(),
                        key,
                        file.getOriginalFilename(),
                        file.getContentType()));
            }
        }

        log.info(
                "Visitor profile saved. visitorId={} existingVisitor={} uploadedImages={} replaceImages={}",
                visitor.getId(),
                existingVisitor,
                nonEmptyFileCount(files),
                replaceImages);

        return toVisitor(visitor);
    }

    @Transactional
    public StartView startConversation(String inviteCode, String browserToken) {
        requireBrowserToken(browserToken);
        if (inviteCode == null || inviteCode.isBlank()) {
            throw new BusinessRuleException("Invite code is required");
        }

        var owner = accounts.findByInviteCode(inviteCode)
                .orElseThrow(() -> new ResourceNotFoundException("Invite link not found"));

        var visitor = visitors.findByBrowserTokenHash(tokens.hash(browserToken))
                .orElseThrow(() -> new BusinessRuleException("Complete your profile first"));

        var conversation = conversations.save(new Conversation(owner.getId(), visitor.getId()));
        log.info(
                "Conversation created. conversationId={} ownerId={} visitorId={} inviteCode={}",
                conversation.getId(),
                owner.getId(),
                visitor.getId(),
                inviteCode);

        return new StartView(conversation.getId(), toVisitor(visitor), owner.getDisplayName());
    }

    @Transactional
    public MessageView send(UUID conversationId, SenderType sender, String body) {
        if (conversationId == null) {
            throw new BusinessRuleException("Conversation id is required");
        }
        if (sender == null) {
            throw new BusinessRuleException("Sender type is required");
        }
        if (body == null || body.isBlank()) {
            throw new BusinessRuleException("Message is empty");
        }

        conversations.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        ChatMessage message = messages.save(new ChatMessage(conversationId, sender, body.trim()));
        log.info(
                "Message saved. messageId={} conversationId={} senderType={} bodyLength={}",
                message.getId(),
                conversationId,
                sender,
                body.trim().length());
        return toMessage(message);
    }

    @Transactional(readOnly = true)
    public List<MessageView> history(UUID conversationId) {
        conversations.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        List<MessageView> history = messages.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(this::toMessage)
                .toList();
        log.debug("Conversation history loaded. conversationId={} messageCount={}", conversationId, history.size());
        return history;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> inbox(String inviteCode) {
        var owner = accounts.findByInviteCode(inviteCode)
                .orElseThrow(() -> new ResourceNotFoundException("Owner invite code not found"));

        List<Map<String, Object>> inbox = conversations.findByOwnerIdOrderByCreatedAtDesc(owner.getId())
                .stream()
                .map(conversation -> {
                    var visitor = visitors.findById(conversation.getVisitorId())
                            .orElseThrow(() -> new ResourceNotFoundException("Visitor profile not found"));
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("conversationId", conversation.getId());
                    item.put("visitorName", visitor.getDisplayName());
                    item.put("createdAt", conversation.getCreatedAt());
                    item.put("images", toVisitor(visitor).imageUrls());
                    return item;
                })
                .toList();

        log.debug("Owner inbox loaded. ownerId={} conversationCount={}", owner.getId(), inbox.size());
        return inbox;
    }

    private VisitorView toVisitor(VisitorProfile visitor) {
        List<String> urls = images.findByVisitorId(visitor.getId())
                .stream()
                .map(image -> "/api/images/" + image.getStorageKey())
                .toList();
        return new VisitorView(visitor.getId(), visitor.getDisplayName(), urls);
    }

    private MessageView toMessage(ChatMessage message) {
        return new MessageView(
                message.getId(),
                message.getSenderType().name(),
                message.getBody(),
                message.getCreatedAt().toString());
    }

    private void requireBrowserToken(String browserToken) {
        if (browserToken == null || browserToken.isBlank()) {
            throw new BusinessRuleException("Browser token is required");
        }
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessRuleException("Name is required");
        }
        if (name.trim().length() > 100) {
            throw new BusinessRuleException("Name cannot exceed 100 characters");
        }
    }

    private void validateImages(List<MultipartFile> files) {
        if (files == null) {
            return;
        }
        if (nonEmptyFileCount(files) > MAX_IMAGES) {
            throw new BusinessRuleException("Maximum 4 images");
        }
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new BusinessRuleException("Only image files are allowed");
            }
        }
    }

    private long nonEmptyFileCount(List<MultipartFile> files) {
        return files == null ? 0 : files.stream().filter(file -> file != null && !file.isEmpty()).count();
    }
}
