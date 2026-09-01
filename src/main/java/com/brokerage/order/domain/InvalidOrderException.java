package com.brokerage.order.domain;

import com.brokerage.common.domain.DomainException;

public class InvalidOrderException extends DomainException {

    public InvalidOrderException(String message) {
        super("INVALID_ORDER", message);
    }
}
