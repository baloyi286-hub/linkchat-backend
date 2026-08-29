package com.linkchat.interfaces.websocket;

import com.linkchat.application.ChatApplicationService;
import com.linkchat.application.OwnerNotificationService;
import com.linkchat.application.exception.BusinessRuleException;
import com.linkchat.domain.model.SenderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
public class ChatSocketController {

    private static final Logger log = LoggerFactory.getLogger(ChatSocketController.class);

    private final ChatApplicationService app;
    private final OwnerNotificationService notifications;
    private final SimpMessagingTemplate broker;

    public ChatSocketController(
            ChatApplicationService app,
            OwnerNotificationService notifications,
            SimpMessagingTemplate broker) {
        this.app = app;
        this.notifications = notifications;
        this.broker = broker;
    }

    public record SocketMessage(String senderType, String body) {
    }

    @MessageMapping("/chat/{conversationId}")
    public void message(@DestinationVariable UUID conversationId, SocketMessage request) {
        SenderType senderType = parseSenderType(request.senderType());
        var saved = app.send(conversationId, senderType, request.body());
        broker.convertAndSend("/topic/conversations/" + conversationId, saved);

        if (senderType == SenderType.VISITOR) {
            notifications.notifyOwnerOfVisitorMessage(conversationId, saved.body());
        }

        log.debug("WebSocket message published. conversationId={} messageId={}", conversationId, saved.id());
    }

    private SenderType parseSenderType(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException("senderType is required");
        }
        try {
            return SenderType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException("senderType must be OWNER or VISITOR");
        }
    }
}
