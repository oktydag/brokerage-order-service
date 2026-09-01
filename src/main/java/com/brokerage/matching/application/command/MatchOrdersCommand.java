package com.brokerage.matching.application.command;

import java.util.List;
import java.util.UUID;

public record MatchOrdersCommand(List<UUID> orderIds) {
}
