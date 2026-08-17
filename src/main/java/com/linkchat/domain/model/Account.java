package com.linkchat.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account")
public class Account {
    @Id
    private UUID id;
    @Column(name = "display_name", nullable = false)
    private String displayName;
    @Column(name = "invite_code", nullable = false, unique = true)
    private String inviteCode;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Account() {
    }

    private Account(
            UUID id,
            String displayName,
            String inviteCode,
            Instant createdAt) {

        this.id = id;
        this.displayName = displayName;
        this.inviteCode = inviteCode;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public static Account create(
            String displayName,
            String inviteCode) {

        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException(
                    "Display name is required");
        }

        if (inviteCode == null || inviteCode.isBlank()) {
            throw new IllegalArgumentException(
                    "Invite code is required");
        }

        return new Account(
                UUID.randomUUID(),
                displayName.trim(),
                inviteCode,
                Instant.now());
    }
}
