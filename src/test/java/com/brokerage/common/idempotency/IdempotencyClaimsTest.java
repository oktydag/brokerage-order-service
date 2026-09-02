package com.brokerage.common.idempotency;

import com.brokerage.common.domain.valueobjects.CustomerId;
import com.brokerage.common.domain.valueobjects.IdempotencyKey;
import com.brokerage.common.domain.valueobjects.RequestFingerprint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyClaimsTest {

    private static final CustomerId CUSTOMER = CustomerId.of("CUST-1");
    private static final IdempotencyKey KEY = IdempotencyKey.of("K1");
    private static final RequestFingerprint FINGERPRINT = RequestFingerprint.over("a");
    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    @Mock
    private IdempotencyRecordRepository records;

    private IdempotencyClaims claims;

    @BeforeEach
    void setUp() {
        claims = new IdempotencyClaims(records, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void doesNothingWhenTheClientSentNoKey() {
        claims.claim(CUSTOMER, null, FINGERPRINT, UUID.randomUUID());

        verify(records, never()).saveAndFlush(any());
        assertThat(claims.resolve(CUSTOMER, null, FINGERPRINT)).isEmpty();
        verify(records, never()).findByCustomerIdAndIdempotencyKey(any(), any());
    }

    @Test
    void writesTheClaimAgainstTheUniqueConstraintImmediately() {
        UUID resourceId = UUID.randomUUID();

        claims.claim(CUSTOMER, KEY, FINGERPRINT, resourceId);

        verify(records).saveAndFlush(any(IdempotencyRecord.class));
    }

    @Test
    void translatesAConstraintViolationIntoADuplicate() {
        when(records.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("unique"));

        assertThatThrownBy(() -> claims.claim(CUSTOMER, KEY, FINGERPRINT, UUID.randomUUID()))
                .isInstanceOf(DuplicateRequestException.class)
                .hasMessageContaining("K1");
    }

    @Test
    void translatesALockTimeoutOnTheClaimIntoADuplicate() {
        when(records.saveAndFlush(any())).thenThrow(new PessimisticLockingFailureException("timeout"));

        assertThatThrownBy(() -> claims.claim(CUSTOMER, KEY, FINGERPRINT, UUID.randomUUID()))
                .isInstanceOf(DuplicateRequestException.class);
    }

    @Test
    void resolvesTheClaimedResourceForAnIdenticalRequest() {
        UUID resourceId = UUID.randomUUID();
        when(records.findByCustomerIdAndIdempotencyKey(CUSTOMER, KEY)).thenReturn(
                Optional.of(IdempotencyRecord.claim(CUSTOMER, KEY, FINGERPRINT, resourceId, NOW)));

        assertThat(claims.resolve(CUSTOMER, KEY, FINGERPRINT)).contains(resourceId);
    }

    @Test
    void resolvesToNothingForAnUnseenKey() {
        when(records.findByCustomerIdAndIdempotencyKey(CUSTOMER, KEY)).thenReturn(Optional.empty());

        assertThat(claims.resolve(CUSTOMER, KEY, FINGERPRINT)).isEmpty();
    }

    @Test
    void rejectsAKeyReusedForADifferentRequest() {
        when(records.findByCustomerIdAndIdempotencyKey(CUSTOMER, KEY)).thenReturn(
                Optional.of(IdempotencyRecord.claim(CUSTOMER, KEY, FINGERPRINT, UUID.randomUUID(), NOW)));

        assertThatThrownBy(() -> claims.resolve(CUSTOMER, KEY, RequestFingerprint.over("b")))
                .isInstanceOf(IdempotencyKeyReuseException.class);
    }
}
