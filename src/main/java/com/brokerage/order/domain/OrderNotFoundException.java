package com.brokerage.order.domain;

import com.brokerage.common.domain.DomainException;

import java.util.UUID;

public class OrderNotFoundException extends DomainException {

    public OrderNotFoundException(UUID orderId) {
        super("ORDER_NOT_FOUND", "Order %s does not exist".formatted(orderId));
    }
}
