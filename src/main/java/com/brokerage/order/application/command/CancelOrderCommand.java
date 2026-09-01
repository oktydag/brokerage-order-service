package com.brokerage.order.application.command;

import com.brokerage.common.domain.valueobjects.AccessScope;

import java.util.UUID;

public record CancelOrderCommand(UUID orderId, AccessScope scope) {
}
