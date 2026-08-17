package com.linkchat.domain.model;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="conversation")
public class Conversation { @Id private UUID id; @Column(name="owner_id",nullable=false) private UUID ownerId; @Column(name="visitor_id",nullable=false) private UUID visitorId; @Column(name="created_at",nullable=false) private Instant createdAt; @Enumerated(EnumType.STRING) @Column(nullable=false) private ConversationStatus status;
protected Conversation(){} public Conversation(UUID owner,UUID visitor){id=UUID.randomUUID();ownerId=owner;visitorId=visitor;createdAt=Instant.now();status=ConversationStatus.OPEN;} public UUID getId(){return id;} public UUID getOwnerId(){return ownerId;} public UUID getVisitorId(){return visitorId;} public Instant getCreatedAt(){return createdAt;} }
