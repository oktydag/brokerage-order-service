package com.brokerage.common.idempotency;

import com.brokerage.common.domain.valueobjects.CustomerId;
import com.brokerage.common.domain.valueobjects.IdempotencyKey;
import com.brokerage.common.domain.valueobjects.RequestFingerprint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private CustomerId customerId;

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private IdempotencyKey idempotencyKey;

    @Column(name = "fingerprint", nullable = false, updatable = false)
    private RequestFingerprint fingerprint;

    @Column(name = "resource_id", nullable = false, updatable = false)
    private UUID resourceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IdempotencyRecord() {
    }

    private IdempotencyRecord(CustomerId customerId, IdempotencyKey idempotencyKey,
                              RequestFingerprint fingerprint, UUID resourceId, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.customerId = customerId;
        this.idempotencyKey = idempotencyKey;
        this.fingerprint = fingerprint;
        this.resourceId = resourceId;
        this.createdAt = createdAt;
    }

    public static IdempotencyRecord claim(CustomerId customerId, IdempotencyKey idempotencyKey,
                                          RequestFingerprint fingerprint, UUID resourceId,
                                          Instant createdAt) {
        return new IdempotencyRecord(customerId, idempotencyKey, fingerprint, resourceId, createdAt);
    }

    public UUID requireSameRequest(RequestFingerprint candidate) {
        if (!fingerprint.equals(candidate)) {
            throw new IdempotencyKeyReuseException(idempotencyKey);
        }
        return resourceId;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
