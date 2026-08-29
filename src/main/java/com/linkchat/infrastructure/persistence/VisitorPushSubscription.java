package com.linkchat.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "visitor_push_subscription")
public class VisitorPushSubscription {

    @Id
    private UUID id;

    @Column(name = "visitor_id", nullable = false)
    private UUID visitorId;

    @Column(name = "firebase_installation_id", nullable = false, unique = true, length = 255)
    private String firebaseInstallationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected VisitorPushSubscription() {
    }

    public VisitorPushSubscription(UUID visitorId, String firebaseInstallationId) {
        this.id = UUID.randomUUID();
        this.visitorId = visitorId;
        this.firebaseInstallationId = firebaseInstallationId;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getVisitorId() {
        return visitorId;
    }

    public String getFirebaseInstallationId() {
        return firebaseInstallationId;
    }

    public void moveToVisitor(UUID newVisitorId) {
        this.visitorId = newVisitorId;
        this.updatedAt = Instant.now();
    }
}
