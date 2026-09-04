package com.linkchat.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account")
public class Account {
    @Id private UUID id;
    @Column(name = "display_name", nullable = false) private String displayName;
    @Column(name = "invite_code", nullable = false, unique = true) private String inviteCode;
    @Column(name = "auth_subject", unique = true) private String authSubject;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "vault_password_hash") private String vaultPasswordHash;

    protected Account() {}

    private Account(UUID id, String displayName, String inviteCode, String authSubject, Instant createdAt) {
        this.id = id; this.displayName = displayName; this.inviteCode = inviteCode;
        this.authSubject = authSubject; this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getInviteCode() { return inviteCode; }
    public String getAuthSubject() { return authSubject; }
    public String getVaultPasswordHash() { return vaultPasswordHash; }
    public void setVaultPasswordHash(String hash) { this.vaultPasswordHash = hash; }

    public static Account create(String displayName, String inviteCode) { return create(displayName, inviteCode, null); }
    public static Account create(String displayName, String inviteCode, String authSubject) {
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("Display name is required");
        if (inviteCode == null || inviteCode.isBlank()) throw new IllegalArgumentException("Invite code is required");
        if (authSubject != null && authSubject.isBlank()) throw new IllegalArgumentException("Auth subject cannot be blank");
        return new Account(UUID.randomUUID(), displayName.trim(), inviteCode, authSubject, Instant.now());
    }
}
