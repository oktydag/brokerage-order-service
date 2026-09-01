package com.brokerage.order.domain;

public record Settlement(Reservation outgoing, Reservation incoming) {
}
