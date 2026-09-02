package com.brokerage.common.idempotency;

import com.brokerage.common.domain.valueobjects.CustomerId;
import com.brokerage.common.domain.valueobjects.IdempotencyKey;
import com.brokerage.common.domain.valueobjects.RequestFingerprint;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotencyRecordTest {

    private static final CustomerId CUSTOMER = CustomerId.of("CUST-1");
    private static final IdempotencyKey KEY = IdempotencyKey.of("K1");
    private static final RequestFingerprint FINGERPRINT = RequestFingerprint.over("a");
    private static final Instant CREATED = Instant.parse("2026-01-15T10:00:00Z");

    private IdempotencyRecord record(UUID resourceId) {
        return IdempotencyRecord.claim(CUSTOMER, KEY, FINGERPRINT, resourceId, CREATED);
    }

    @Test
    void returnsTheClaimedResourceForAnIdenticalRequest() {
        UUID resourceId = UUID.randomUUID();

        assertThat(record(resourceId).requireSameRequest(FINGERPRINT)).isEqualTo(resourceId);
    }

    @Test
    void rejectsTheSameKeyUsedForADifferentRequest() {
        IdempotencyRecord claimed = record(UUID.randomUUID());
        RequestFingerprint other = RequestFingerprint.over("b");

        assertThatThrownBy(() -> claimed.requireSameRequest(other))
                .isInstanceOf(IdempotencyKeyReuseException.class)
                .hasMessageContaining("K1");
    }

    @Test
    void exposesItsClaimMetadata() {
        UUID resourceId = UUID.randomUUID();
        IdempotencyRecord claimed = record(resourceId);

        assertThat(claimed.getResourceId()).isEqualTo(resourceId);
        assertThat(claimed.getCreatedAt()).isEqualTo(CREATED);
    }

    @Test
    void exceptionsCarryStableCodes() {
        assertThat(new IdempotencyKeyReuseException(KEY).code()).isEqualTo("IDEMPOTENCY_KEY_REUSE");
        assertThat(new DuplicateRequestException(KEY).code()).isEqualTo("DUPLICATE_REQUEST");
    }
}
