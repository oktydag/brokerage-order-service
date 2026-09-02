package com.brokerage.order.application.command;

import com.brokerage.common.domain.valueobjects.Amount;
import com.brokerage.common.domain.valueobjects.AssetName;
import com.brokerage.common.domain.valueobjects.CustomerId;
import com.brokerage.order.domain.valueobjects.OrderSide;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceOrderCommandTest {

    private PlaceOrderCommand command(String size, String price, OrderSide side) {
        return new PlaceOrderCommand(CustomerId.of("CUST-1"), AssetName.of("THYAO"), side,
                Amount.of(size), Amount.of(price), null);
    }

    @Test
    void fingerprintsEquivalentAmountsIdentically() {
        assertThat(command("100", "300", OrderSide.BUY).fingerprint())
                .isEqualTo(command("100.00", "300.0000", OrderSide.BUY).fingerprint());
    }

    @Test
    void fingerprintChangesWithTheOrderDetails() {
        assertThat(command("100", "300", OrderSide.BUY).fingerprint())
                .isNotEqualTo(command("101", "300", OrderSide.BUY).fingerprint())
                .isNotEqualTo(command("100", "301", OrderSide.BUY).fingerprint())
                .isNotEqualTo(command("100", "300", OrderSide.SELL).fingerprint());
    }
}
