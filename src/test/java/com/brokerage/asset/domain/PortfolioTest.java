package com.brokerage.asset.domain;

import com.brokerage.common.domain.valueobjects.Amount;
import com.brokerage.common.domain.valueobjects.AssetName;
import com.brokerage.common.domain.valueobjects.CustomerId;
import com.brokerage.common.domain.valueobjects.Reservation;
import com.brokerage.common.domain.valueobjects.Settlement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PortfolioTest {

    private static final CustomerId CUSTOMER = CustomerId.of("CUST-1");
    private static final AssetName THYAO = AssetName.of("THYAO");

    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        portfolio = Portfolio.empty(CUSTOMER);
        portfolio.deposit(AssetName.TRY, Amount.of(100_000));
        portfolio.deposit(THYAO, Amount.of(500));
    }

    @Test
    void startsEmptyAndKnowsItsOwner() {
        Portfolio empty = Portfolio.empty(CUSTOMER);

        assertThat(empty.customerId()).isEqualTo(CUSTOMER);
        assertThat(empty.holdings()).isEmpty();
        assertThat(empty.holding(THYAO)).isEmpty();
    }

    @Test
    void depositOpensAHoldingAndTracksItForPersistence() {
        assertThat(portfolio.holdings()).hasSize(2);
        assertThat(portfolio.newlyCreated()).hasSize(2);
        assertThat(portfolio.holding(THYAO)).isPresent();
    }

    @Test
    void depositAccumulatesIntoAnExistingHolding() {
        portfolio.deposit(THYAO, Amount.of(100));

        assertThat(portfolio.holding(THYAO).orElseThrow().getSize()).isEqualTo(Amount.of(600));
        assertThat(portfolio.newlyCreated()).hasSize(2);
    }

    @Test
    void reserveAndReleaseMoveTheUsableBalance() {
        portfolio.reserve(new Reservation(AssetName.TRY, Amount.of(30_000)));
        assertThat(portfolio.holding(AssetName.TRY).orElseThrow().getUsableSize())
                .isEqualTo(Amount.of(70_000));

        portfolio.release(new Reservation(AssetName.TRY, Amount.of(30_000)));
        assertThat(portfolio.holding(AssetName.TRY).orElseThrow().getUsableSize())
                .isEqualTo(Amount.of(100_000));
    }

    @Test
    void settleMovesBothLegsAtOnce() {
        portfolio.reserve(new Reservation(AssetName.TRY, Amount.of(30_000)));

        portfolio.settle(new Settlement(
                new Reservation(AssetName.TRY, Amount.of(30_000)),
                new Reservation(THYAO, Amount.of(100))));

        Asset cash = portfolio.holding(AssetName.TRY).orElseThrow();
        Asset stock = portfolio.holding(THYAO).orElseThrow();
        assertThat(cash.getSize()).isEqualTo(Amount.of(70_000));
        assertThat(cash.getUsableSize()).isEqualTo(Amount.of(70_000));
        assertThat(stock.getSize()).isEqualTo(Amount.of(600));
        assertThat(stock.getUsableSize()).isEqualTo(Amount.of(600));
    }

    @Test
    void settleOpensAHoldingForAnAssetTheCustomerDidNotOwn() {
        AssetName aselsan = AssetName.of("ASELS");
        portfolio.reserve(new Reservation(AssetName.TRY, Amount.of(10_000)));

        portfolio.settle(new Settlement(
                new Reservation(AssetName.TRY, Amount.of(10_000)),
                new Reservation(aselsan, Amount.of(50))));

        assertThat(portfolio.holding(aselsan).orElseThrow().getSize()).isEqualTo(Amount.of(50));
        assertThat(portfolio.newlyCreated()).hasSize(3);
    }

    @Test
    void refusesToDrawOnAnAssetTheCustomerDoesNotHold() {
        Reservation missing = new Reservation(AssetName.of("GARAN"), Amount.of(1));

        assertThatThrownBy(() -> portfolio.reserve(missing))
                .isInstanceOf(AssetNotHeldException.class)
                .hasMessageContaining("GARAN");
        assertThatThrownBy(() -> portfolio.release(missing))
                .isInstanceOf(AssetNotHeldException.class);
    }

    @Test
    void loadsFromExistingHoldingsWithoutMarkingThemNew() {
        Asset existing = Asset.open(CUSTOMER, THYAO);
        existing.credit(Amount.of(10));

        Portfolio loaded = Portfolio.of(CUSTOMER, List.of(existing));

        assertThat(loaded.holdings()).hasSize(1);
        assertThat(loaded.newlyCreated()).isEmpty();
    }
}
