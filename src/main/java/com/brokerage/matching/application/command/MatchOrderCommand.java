package com.brokerage.matching.application.command;

import java.util.UUID;

public record MatchOrderCommand(UUID orderId) {
}
