package com.brokerage.order.domain;

import com.brokerage.common.domain.DomainException;

import java.util.UUID;

public class IllegalOrderTransitionException extends DomainException {

    public IllegalOrderTransitionException(UUID orderId, OrderStatus from, OrderStatus to) {
        super("ILLEGAL_ORDER_TRANSITION",
                "Order %s is %s and cannot become %s".formatted(orderId, from, to));
    }
}
