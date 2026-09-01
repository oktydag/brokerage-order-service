package com.brokerage.matching.web;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record MatchOrdersRequest(
        @NotEmpty @Size(max = 500) List<UUID> orderIds) {
}
