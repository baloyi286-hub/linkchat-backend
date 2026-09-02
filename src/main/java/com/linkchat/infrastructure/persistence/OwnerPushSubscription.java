package com.linkchat.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "owner_push_subscription")
public class OwnerPushSubscription {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "firebase_installation_id", nullable = false, unique = true, length = 255)
    private String firebaseInstallationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OwnerPushSubscription() {
    }

    public OwnerPushSubscription(UUID accountId, String firebaseInstallationId) {
        this.id = UUID.randomUUID();
        this.accountId = accountId;
        this.firebaseInstallationId = firebaseInstallationId;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getFirebaseInstallationId() {
        return firebaseInstallationId;
    }

    public void moveToAccount(UUID newAccountId) {
        this.accountId = newAccountId;
        this.updatedAt = Instant.now();
    }
}
