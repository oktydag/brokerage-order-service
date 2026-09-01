package com.brokerage.order.domain.event;

import com.brokerage.order.domain.Order;

import java.time.Instant;
import java.util.UUID;

public record OrderPlaced(UUID orderId, String customerId, String assetName,
                          String orderSide, Instant occurredAt) {

    public static OrderPlaced of(Order order) {
        return new OrderPlaced(order.getId(), order.getCustomerId().value(),
                order.getAssetName().value(), order.getOrderSide().name(), order.getCreateDate());
    }
}
