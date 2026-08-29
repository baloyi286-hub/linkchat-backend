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
    private final OwnerPushSubscriptionRepository subscriptions;
    private final ObjectProvider<FirebaseMessaging> messagingProvider;
    private final String frontendUrl;

    public OwnerNotificationService(
            AccountRepository accounts,
            ConversationRepository conversations,
            VisitorProfileRepository visitors,
            OwnerPushSubscriptionRepository subscriptions,
            ObjectProvider<FirebaseMessaging> messagingProvider,
            @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.accounts = accounts;
        this.conversations = conversations;
        this.visitors = visitors;
        this.subscriptions = subscriptions;
        this.messagingProvider = messagingProvider;
        this.frontendUrl = frontendUrl;
    }

    @Transactional
    public void registerOwner(String authSubject, String installationId) {
        if (installationId == null || installationId.isBlank()) {
            throw new BusinessRuleException("Firebase installation id is required");
        }

        var owner = accounts.findByAuthSubject(authSubject)
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found"));

        OwnerPushSubscription subscription = subscriptions
                .findByFirebaseInstallationId(installationId.trim())
                .orElseGet(() -> new OwnerPushSubscription(owner.getId(), installationId.trim()));

        subscription.moveToAccount(owner.getId());
        subscriptions.save(subscription);

        log.info("Owner push subscription registered. ownerId={} subscriptionId={}",
                owner.getId(), subscription.getId());
    }

    @Async("notificationExecutor")
    public void notifyOwnerOfVisitorMessage(UUID conversationId, String body) {
        FirebaseMessaging messaging = messagingProvider.getIfAvailable();
        if (messaging == null) {
            log.debug("Firebase messaging is not configured; push notification skipped");
            return;
        }

        try {
            var conversation = conversations.findById(conversationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
            var visitor = visitors.findById(conversation.getVisitorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Visitor profile not found"));
            var owner = accounts.findById(conversation.getOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found"));

            var ownerSubscriptions = subscriptions.findByAccountId(conversation.getOwnerId());
            if (ownerSubscriptions.isEmpty()) {
                return;
            }

            String preview = body == null ? "New message" : body.trim();
            if (preview.length() > 120) {
                preview = preview.substring(0, 117) + "...";
            }

            String link = frontendUrl + "/owner/" + owner.getInviteCode()
                    + "?conversation=" + conversationId;

            for (OwnerPushSubscription subscription : ownerSubscriptions) {
                Message message = Message.builder()
                        .setFid(subscription.getFirebaseInstallationId())
                        .setNotification(Notification.builder()
                                .setTitle("New message from " + visitor.getDisplayName())
                                .setBody(preview)
                                .build())
                        .setWebpushConfig(WebpushConfig.builder()
                                .setFcmOptions(WebpushFcmOptions.withLink(link))
                                .build())
                        .putData("conversationId", conversationId.toString())
                        .putData("senderType", "VISITOR")
                        .build();

                try {
                    messaging.send(message);
                } catch (Exception exception) {
                    log.warn("Failed to send owner push notification. subscriptionId={} reason={}",
                            subscription.getId(), exception.getMessage());
                }
            }
        } catch (Exception exception) {
            log.warn("Owner notification processing failed. conversationId={} reason={}",
                    conversationId, exception.getMessage());
        }
    }
}
