package com.linkchat.domain.model;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="visitor_image")
public class VisitorImage { @Id private UUID id; @Column(name="visitor_id",nullable=false) private UUID visitorId; @Column(name="storage_key",nullable=false) private String storageKey; @Column(name="original_name") private String originalName; @Column(name="content_type") private String contentType; @Column(name="created_at",nullable=false) private Instant createdAt;
protected VisitorImage(){} public VisitorImage(UUID visitorId,String key,String original,String type){id=UUID.randomUUID();this.visitorId=visitorId;storageKey=key;originalName=original;contentType=type;createdAt=Instant.now();} public UUID getId(){return id;} public String getStorageKey(){return storageKey;} }
