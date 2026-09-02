package com.brokerage.asset.domain;

import com.brokerage.common.domain.InvariantViolationException;
import com.brokerage.common.domain.valueobjects.Amount;
import com.brokerage.common.domain.valueobjects.AssetName;
import com.brokerage.common.domain.valueobjects.CustomerId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssetTest {

    private static final CustomerId CUSTOMER = CustomerId.of("CUST-1");
    private static final AssetName THYAO = AssetName.of("THYAO");

    private Asset funded(long amount) {
        Asset asset = Asset.open(CUSTOMER, THYAO);
        asset.credit(Amount.of(amount));
        return asset;
    }

    @Test
    void opensEmpty() {
        Asset asset = Asset.open(CUSTOMER, THYAO);

        assertThat(asset.getId()).isNotNull();
        assertThat(asset.getCustomerId()).isEqualTo(CUSTOMER);
        assertThat(asset.getAssetName()).isEqualTo(THYAO);
        assertThat(asset.getSize()).isEqualTo(Amount.ZERO);
        assertThat(asset.getUsableSize()).isEqualTo(Amount.ZERO);
        assertThat(asset.getReservedSize()).isEqualTo(Amount.ZERO);
    }

    @Test
    void creditMakesTheAmountImmediatelySpendable() {
        Asset asset = funded(100);

        assertThat(asset.getSize()).isEqualTo(Amount.of(100));
        assertThat(asset.getUsableSize()).isEqualTo(Amount.of(100));
    }

    @Test
    void reserveHoldsBackUsableBalanceWithoutChangingOwnership() {
        Asset asset = funded(100);

        asset.reserve(Amount.of(30));

        assertThat(asset.getSize()).isEqualTo(Amount.of(100));
        assertThat(asset.getUsableSize()).isEqualTo(Amount.of(70));
        assertThat(asset.getReservedSize()).isEqualTo(Amount.of(30));
    }

    @Test
    void reserveRefusesToOverdrawTheUsableBalance() {
        Asset asset = funded(100);
        asset.reserve(Amount.of(80));

        assertThatThrownBy(() -> asset.reserve(Amount.of(21)))
                .isInstanceOf(InsufficientUsableBalanceException.class)
                .hasMessageContaining("CUST-1");

        assertThat(asset.getUsableSize()).isEqualTo(Amount.of(20));
    }

    @Test
    void reserveExposesTheShortfallOnTheException() {
        Asset asset = funded(100);

        InsufficientUsableBalanceException thrown = org.junit.jupiter.api.Assertions.assertThrows(
                InsufficientUsableBalanceException.class, () -> asset.reserve(Amount.of(101)));

        assertThat(thrown.code()).isEqualTo("INSUFFICIENT_USABLE_BALANCE");
        assertThat(thrown.customerId()).isEqualTo(CUSTOMER);
        assertThat(thrown.assetName()).isEqualTo(THYAO);
        assertThat(thrown.required()).isEqualTo(Amount.of(101));
        assertThat(thrown.available()).isEqualTo(Amount.of(100));
    }

    @Test
    void releaseReturnsTheReservationToTheUsableBalance() {
        Asset asset = funded(100);
        asset.reserve(Amount.of(40));

        asset.release(Amount.of(40));

        assertThat(asset.getUsableSize()).isEqualTo(Amount.of(100));
        assertThat(asset.getReservedSize()).isEqualTo(Amount.ZERO);
    }

    @Test
    void releaseBeyondWhatWasReservedBreaksTheInvariant() {
        Asset asset = funded(100);

        assertThatThrownBy(() -> asset.release(Amount.of(1)))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("usableSize");
    }

    @Test
    void debitSettlesTheReservedLegWithoutTouchingUsableSize() {
        Asset asset = funded(100);
        asset.reserve(Amount.of(30));

        asset.debit(Amount.of(30));

        assertThat(asset.getSize()).isEqualTo(Amount.of(70));
        assertThat(asset.getUsableSize()).isEqualTo(Amount.of(70));
        assertThat(asset.getReservedSize()).isEqualTo(Amount.ZERO);
    }

    @Test
    void debitBeyondTheReservationBreaksTheInvariant() {
        Asset asset = funded(100);

        assertThatThrownBy(() -> asset.debit(Amount.of(10)))
                .isInstanceOf(InvariantViolationException.class);
    }
}
