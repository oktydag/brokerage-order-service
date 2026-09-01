package com.brokerage.common.domain;

public class InvariantViolationException extends DomainException {

    public InvariantViolationException(String message) {
        super("INVARIANT_VIOLATION", message);
    }
}
