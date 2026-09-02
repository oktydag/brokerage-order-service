package com.brokerage.common.idempotency;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyRecordCleanerTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    @Mock
    private IdempotencyRecordRepository records;

    private IdempotencyRecordCleaner cleanerWith(Duration retention) {
        return new IdempotencyRecordCleaner(records, new IdempotencyProperties(retention),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void purgesClaimsOlderThanTheRetentionWindow() {
        when(records.deleteOlderThan(NOW.minus(Duration.ofDays(1)))).thenReturn(3);

        cleanerWith(Duration.ofDays(1)).purgeExpired();

        verify(records).deleteOlderThan(NOW.minus(Duration.ofDays(1)));
    }

    @Test
    void staysQuietWhenThereIsNothingToPurge() {
        when(records.deleteOlderThan(NOW.minus(Duration.ofHours(6)))).thenReturn(0);

        cleanerWith(Duration.ofHours(6)).purgeExpired();

        verify(records).deleteOlderThan(NOW.minus(Duration.ofHours(6)));
    }

    @Test
    void defaultsTheRetentionWindowWhenUnconfigured() {
        assertThat(new IdempotencyProperties(null).retention()).isEqualTo(Duration.ofDays(1));
    }
}
