package com.linkchat.domain.model;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="owner_image")
public class OwnerImage {
 @Id private UUID id; @Column(name="owner_id",nullable=false) private UUID ownerId; @Column(name="storage_key",nullable=false) private String storageKey; @Column(name="original_name") private String originalName; @Column(name="content_type") private String contentType; @Column(name="created_at",nullable=false) private Instant createdAt;
 protected OwnerImage(){} public OwnerImage(UUID ownerId,String key,String name,String type){id=UUID.randomUUID();this.ownerId=ownerId;storageKey=key;originalName=name;contentType=type;createdAt=Instant.now();}
 public String getStorageKey(){return storageKey;}
}