package com.brokerage.common.domain;

public record Settlement(Reservation outgoing, Reservation incoming) {
}
