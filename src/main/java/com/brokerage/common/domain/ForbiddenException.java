package com.brokerage.common.domain;

public class ForbiddenException extends DomainException {

    public ForbiddenException(String message) {
        super("FORBIDDEN", message);
    }
}
