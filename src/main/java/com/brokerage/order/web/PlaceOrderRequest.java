package com.brokerage.order.web;

import com.brokerage.order.domain.OrderSide;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PlaceOrderRequest(
        @Size(max = 64) String customerId,

        @NotBlank @Size(max = 32) String assetName,

        @NotNull OrderSide orderSide,

        @NotNull @Positive @Digits(integer = 30, fraction = 8) BigDecimal size,

        @NotNull @Positive @Digits(integer = 30, fraction = 8) BigDecimal price) {
}
