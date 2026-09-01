package com.brokerage.common.idempotency;

import com.brokerage.common.domain.DomainException;
import com.brokerage.common.domain.valueobjects.IdempotencyKey;

public class IdempotencyKeyReuseException extends DomainException {

    public IdempotencyKeyReuseException(IdempotencyKey key) {
        super("IDEMPOTENCY_KEY_REUSE",
                "Idempotency key %s was already used for a different request".formatted(key));
    }
}
