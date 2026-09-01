package com.brokerage.common.domain.valueobjects;

public record Settlement(Reservation outgoing, Reservation incoming) {
}
