package com.brokerage.common.idempotency;

import com.brokerage.common.domain.valueobjects.CustomerId;
import com.brokerage.common.domain.valueobjects.IdempotencyKey;
import com.brokerage.common.domain.valueobjects.RequestFingerprint;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

@Component
public class IdempotencyClaims {

    private final IdempotencyRecordRepository records;
    private final Clock clock;

    public IdempotencyClaims(IdempotencyRecordRepository records, Clock clock) {
        this.records = records;
        this.clock = clock;
    }

    public void claim(CustomerId customerId, IdempotencyKey key,
                      RequestFingerprint fingerprint, UUID resourceId) {
        if (key == null) {
            return;
        }
        try {
            records.saveAndFlush(IdempotencyRecord.claim(
                    customerId, key, fingerprint, resourceId, clock.instant()));
        } catch (DataIntegrityViolationException | PessimisticLockingFailureException e) {
            throw new DuplicateRequestException(key);
        }
    }

    @Transactional(readOnly = true)
    public Optional<UUID> resolve(CustomerId customerId, IdempotencyKey key,
                                  RequestFingerprint fingerprint) {
        if (key == null) {
            return Optional.empty();
        }
        return records.findByCustomerIdAndIdempotencyKey(customerId, key)
                .map(record -> record.requireSameRequest(fingerprint));
    }
}
