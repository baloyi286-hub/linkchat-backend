package com.linkchat.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VisitorPushSubscriptionRepository extends JpaRepository<VisitorPushSubscription, UUID> {
    List<VisitorPushSubscription> findByVisitorId(UUID visitorId);
    Optional<VisitorPushSubscription> findByFirebaseInstallationId(String firebaseInstallationId);
}
