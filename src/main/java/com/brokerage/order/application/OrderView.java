package com.brokerage.order.application;

import com.brokerage.order.domain.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderView(
        UUID id,
        String customerId,
        String assetName,
        String orderSide,
        BigDecimal size,
        BigDecimal price,
        BigDecimal totalValue,
        String status,
        Instant createDate) {

    public static OrderView from(Order order) {
        return new OrderView(
                order.getId(),
                order.getCustomerId().value(),
                order.getAssetName().value(),
                order.getOrderSide().name(),
                order.getSize().toPlainBigDecimal(),
                order.getPrice().toPlainBigDecimal(),
                order.totalValue().toPlainBigDecimal(),
                order.getStatus().name(),
                order.getCreateDate());
    }
}
