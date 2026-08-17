package com.linkchat.domain.model;
import jakarta.persistence.*; import java.time.Instant; import java.util.*;
@Entity @Table(name="visitor_profile")
public class VisitorProfile { @Id private UUID id; @Column(name="browser_token_hash",nullable=false,unique=true) private String browserTokenHash; @Column(name="display_name",nullable=false) private String displayName; @Column(name="created_at",nullable=false) private Instant createdAt; @Column(name="updated_at",nullable=false) private Instant updatedAt;
protected VisitorProfile(){} public VisitorProfile(String hash,String name){id=UUID.randomUUID();browserTokenHash=hash;displayName=name;createdAt=updatedAt=Instant.now();} public void rename(String n){displayName=n;updatedAt=Instant.now();} public UUID getId(){return id;} public String getDisplayName(){return displayName;} }
