package com.linkchat.domain.model;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="chat_message")
public class ChatMessage { @Id private UUID id; @Column(name="conversation_id",nullable=false) private UUID conversationId; @Enumerated(EnumType.STRING) @Column(name="sender_type",nullable=false) private SenderType senderType; @Column(nullable=false,columnDefinition="text") private String body; @Column(name="created_at",nullable=false) private Instant createdAt;
protected ChatMessage(){} public ChatMessage(UUID c,SenderType s,String b){id=UUID.randomUUID();conversationId=c;senderType=s;body=b;createdAt=Instant.now();} public UUID getId(){return id;} public UUID getConversationId(){return conversationId;} public SenderType getSenderType(){return senderType;} public String getBody(){return body;} public Instant getCreatedAt(){return createdAt;} }
