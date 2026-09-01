package com.brokerage.common.idempotency;

import com.brokerage.common.domain.DomainException;
import com.brokerage.common.domain.valueobjects.IdempotencyKey;

public class DuplicateRequestException extends DomainException {

    public DuplicateRequestException(IdempotencyKey key) {
        super("DUPLICATE_REQUEST",
                "A request with idempotency key %s is already in progress".formatted(key));
    }
}
