package com.brokerage.order.application;

import com.brokerage.common.domain.AssetName;
import com.brokerage.common.domain.CustomerId;
import com.brokerage.order.domain.OrderSide;
import com.brokerage.order.domain.OrderStatus;

import java.time.Instant;
import java.util.Set;

public record OrderQuery(
        CustomerId customerId,
        Instant from,
        Instant to,
        Set<OrderStatus> statuses,
        AssetName assetName,
        OrderSide orderSide) {
}
