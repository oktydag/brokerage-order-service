package com.brokerage.common.idempotency;

import com.brokerage.common.domain.valueobjects.CustomerId;
import com.brokerage.common.domain.valueobjects.IdempotencyKey;
import com.brokerage.common.domain.valueobjects.RequestFingerprint;
import com.brokerage.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyRetentionIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private IdempotencyRecordRepository records;

    @Autowired
    private Clock clock;

    private UUID storeClaim(Duration age) {
        CustomerId customerId = CustomerId.of("RET-" + UUID.randomUUID());
        UUID resourceId = UUID.randomUUID();
        transactions.executeWithoutResult(status -> records.save(IdempotencyRecord.claim(
                customerId,
                IdempotencyKey.of("K-" + UUID.randomUUID()),
                RequestFingerprint.over("a"),
                resourceId,
                clock.instant().minus(age))));
        return resourceId;
    }

    @Test
    void aFreshClaimSurvivesTheDefaultRetentionWindow() {
        UUID fresh = storeClaim(Duration.ZERO);
        UUID recent = storeClaim(Duration.ofHours(23));

        int purged = transactions.execute(status ->
                records.deleteOlderThan(clock.instant().minus(Duration.ofDays(1))));

        assertThat(purged).isZero();
        assertThat(records.findAll())
                .extracting(IdempotencyRecord::getResourceId)
                .contains(fresh, recent);
    }

    @Test
    void aClaimBeyondTheWindowIsPurged() {
        UUID stale = storeClaim(Duration.ofDays(2));

        transactions.execute(status ->
                records.deleteOlderThan(clock.instant().minus(Duration.ofDays(1))));

        assertThat(records.findAll())
                .extracting(IdempotencyRecord::getResourceId)
                .doesNotContain(stale);
    }
}
