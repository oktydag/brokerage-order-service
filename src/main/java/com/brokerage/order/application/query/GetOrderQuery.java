package com.brokerage.order.application.query;

import com.brokerage.common.domain.valueobjects.AccessScope;

import java.util.UUID;

public record GetOrderQuery(UUID orderId, AccessScope scope) {
}
