package com.brokerage.order.domain.event;

import com.brokerage.order.domain.Order;

import java.util.UUID;

public record OrderMatched(UUID orderId, String customerId) {

    public static OrderMatched of(Order order) {
        return new OrderMatched(order.getId(), order.getCustomerId().value());
    }
}
