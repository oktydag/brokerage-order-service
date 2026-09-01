package com.brokerage.order.domain.event;

import com.brokerage.order.domain.Order;

import java.util.UUID;

public record OrderCanceled(UUID orderId, String customerId) {

    public static OrderCanceled of(Order order) {
        return new OrderCanceled(order.getId(), order.getCustomerId().value());
    }
}
