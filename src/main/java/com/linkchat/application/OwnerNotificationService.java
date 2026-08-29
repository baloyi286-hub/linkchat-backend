package com.linkchat.application;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushFcmOptions;
import com.linkchat.application.exception.BusinessRuleException;
import com.linkchat.application.exception.ResourceNotFoundException;
import com.linkchat.domain.repository.AccountRepository;
import com.linkchat.domain.repository.ConversationRepository;
import com.linkchat.domain.repository.VisitorProfileRepository;
import com.linkchat.infrastructure.persistence.OwnerPushSubscription;
import com.linkchat.infrastructure.persistence.OwnerPushSubscriptionRepository;
import com.linkchat.infrastructure.persistence.VisitorPushSubscription;
import com.linkchat.infrastructure.persistence.VisitorPushSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OwnerNotificationService {

    private static final Logger log = LoggerFactory.getLogger(OwnerNotificationService.class);

    private final AccountRepository accounts;
    private final ConversationRepository conversations;
    private final VisitorProfileRepository visitors;
    private final OwnerPushSubscriptionRepository ownerSubscriptions;
    private final VisitorPushSubscriptionRepository visitorSubscriptions;
    private final TokenService tokens;
    private final ObjectProvider<FirebaseMessaging> messagingProvider;
    private final String frontendUrl;

    public OwnerNotificationService(
            AccountRepository accounts,
            ConversationRepository conversations,
            VisitorProfileRepository visitors,
            OwnerPushSubscriptionRepository ownerSubscriptions,
            VisitorPushSubscriptionRepository visitorSubscriptions,
            TokenService tokens,
            ObjectProvider<FirebaseMessaging> messagingProvider,
            @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.accounts = accounts;
        this.conversations = conversations;
        this.visitors = visitors;
        this.ownerSubscriptions = ownerSubscriptions;
        this.visitorSubscriptions = visitorSubscriptions;
        this.tokens = tokens;
        this.messagingProvider = messagingProvider;
        this.frontendUrl = frontendUrl;
    }

    @Transactional
    public void registerOwner(String authSubject, String installationId) {
        validateInstallationId(installationId);

        var owner = accounts.findByAuthSubject(authSubject)
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found"));

        OwnerPushSubscription subscription = ownerSubscriptions
                .findByFirebaseInstallationId(installationId.trim())
                .orElseGet(() -> new OwnerPushSubscription(owner.getId(), installationId.trim()));

        subscription.moveToAccount(owner.getId());
        ownerSubscriptions.save(subscription);

        log.info("Owner push subscription registered. ownerId={} subscriptionId={}",
                owner.getId(), subscription.getId());
    }

    @Transactional
    public void registerVisitor(String browserToken, String installationId) {
        if (browserToken == null || browserToken.isBlank()) {
            throw new BusinessRuleException("Browser token is required");
        }
        validateInstallationId(installationId);

        var visitor = visitors.findByBrowserTokenHash(tokens.hash(browserToken))
                .orElseThrow(() -> new ResourceNotFoundException("Visitor profile not found"));

        VisitorPushSubscription subscription = visitorSubscriptions
                .findByFirebaseInstallationId(installationId.trim())
                .orElseGet(() -> new VisitorPushSubscription(visitor.getId(), installationId.trim()));

        subscription.moveToVisitor(visitor.getId());
        visitorSubscriptions.save(subscription);

        log.info("Visitor push subscription registered. visitorId={} subscriptionId={}",
                visitor.getId(), subscription.getId());
    }

    @Async("notificationExecutor")
    public void notifyOwnerOfVisitorMessage(UUID conversationId, String body) {
        FirebaseMessaging messaging = messagingProvider.getIfAvailable();
        if (messaging == null) {
            log.debug("Firebase messaging is not configured; owner push notification skipped");
            return;
        }

        try {
            var conversation = conversations.findById(conversationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
            var visitor = visitors.findById(conversation.getVisitorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Visitor profile not found"));
            var owner = accounts.findById(conversation.getOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found"));

            String link = frontendUrl + "/owner/" + owner.getInviteCode()
                    + "?conversation=" + conversationId;

            for (OwnerPushSubscription subscription : ownerSubscriptions.findByAccountId(owner.getId())) {
                send(
                        messaging,
                        subscription.getFirebaseInstallationId(),
                        "New message from " + visitor.getDisplayName(),
                        preview(body),
                        link,
                        conversationId,
                        "VISITOR",
                        subscription.getId());
            }
        } catch (Exception exception) {
            log.warn("Owner notification processing failed. conversationId={} reason={}",
                    conversationId, exception.getMessage());
        }
    }

    @Async("notificationExecutor")
    public void notifyVisitorOfOwnerMessage(UUID conversationId, String body) {
        FirebaseMessaging messaging = messagingProvider.getIfAvailable();
        if (messaging == null) {
            log.debug("Firebase messaging is not configured; visitor push notification skipped");
            return;
        }

        try {
            var conversation = conversations.findById(conversationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
            var owner = accounts.findById(conversation.getOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found"));

            String link = frontendUrl + "/chat/" + conversationId;

            for (VisitorPushSubscription subscription : visitorSubscriptions.findByVisitorId(conversation.getVisitorId())) {
                send(
                        messaging,
                        subscription.getFirebaseInstallationId(),
                        "New message from " + owner.getDisplayName(),
                        preview(body),
                        link,
                        conversationId,
                        "OWNER",
                        subscription.getId());
            }
        } catch (Exception exception) {
            log.warn("Visitor notification processing failed. conversationId={} reason={}",
                    conversationId, exception.getMessage());
        }
    }

    private void send(
            FirebaseMessaging messaging,
            String installationId,
            String title,
            String body,
            String link,
            UUID conversationId,
            String senderType,
            UUID subscriptionId) {

        Message message = Message.builder()
                .setFid(installationId)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .setWebpushConfig(WebpushConfig.builder()
                        .setFcmOptions(WebpushFcmOptions.withLink(link))
                        .build())
                .putData("conversationId", conversationId.toString())
                .putData("senderType", senderType)
                .build();

        try {
            messaging.send(message);
        } catch (Exception exception) {
            log.warn("Failed to send push notification. subscriptionId={} reason={}",
                    subscriptionId, exception.getMessage());
        }
    }

    private void validateInstallationId(String installationId) {
        if (installationId == null || installationId.isBlank()) {
            throw new BusinessRuleException("Firebase installation id is required");
        }
    }

    private String preview(String body) {
        String value = body == null || body.isBlank() ? "New message" : body.trim();
        return value.length() > 120 ? value.substring(0, 117) + "..." : value;
    }
}
