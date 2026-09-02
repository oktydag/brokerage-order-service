package com.brokerage.common.domain.valueobjects;

import com.brokerage.common.domain.InvariantViolationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AmountTest {

    @Test
    void normalisesToFixedScale() {
        assertThat(Amount.of("100").toBigDecimal()).isEqualByComparingTo("100");
        assertThat(Amount.of("100").toBigDecimal().scale()).isEqualTo(Amount.SCALE);
    }

    @Test
    void treatsDifferentScalesOfTheSameQuantityAsEqual() {
        assertThat(Amount.of("100")).isEqualTo(Amount.of("100.00"));
        assertThat(Amount.of("100")).hasSameHashCodeAs(Amount.of("100.00000000"));
    }

    @Test
    void rejectsNegativeValues() {
        assertThatThrownBy(() -> Amount.of("-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
    }

    @Test
    void rejectsNullValues() {
        assertThatThrownBy(() -> Amount.of((BigDecimal) null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void addsAndSubtracts() {
        assertThat(Amount.of(10).plus(Amount.of(5))).isEqualTo(Amount.of(15));
        assertThat(Amount.of(10).minus(Amount.of(4))).isEqualTo(Amount.of(6));
    }

    @Test
    void refusesToSubtractBelowZero() {
        assertThatThrownBy(() -> Amount.of(5).minus(Amount.of(6)))
                .isInstanceOf(InvariantViolationException.class);
    }

    @Test
    void multipliesSizeByPrice() {
        assertThat(Amount.of("2.5").multipliedBy(Amount.of("4"))).isEqualTo(Amount.of("10"));
    }

    @Test
    void comparesQuantities() {
        assertThat(Amount.of(1).isLessThan(Amount.of(2))).isTrue();
        assertThat(Amount.of(2).isLessThan(Amount.of(2))).isFalse();
        assertThat(Amount.of(3).isGreaterThan(Amount.of(2))).isTrue();
        assertThat(Amount.of(2).isGreaterThan(Amount.of(3))).isFalse();
        assertThat(Amount.ZERO.isZero()).isTrue();
        assertThat(Amount.ZERO.isPositive()).isFalse();
        assertThat(Amount.of(1).isPositive()).isTrue();
        assertThat(Amount.of(1).compareTo(Amount.of(2))).isNegative();
    }

    @Test
    void rendersWithoutTrailingZerosForResponses() {
        assertThat(Amount.of("100.50000000").toPlainBigDecimal()).isEqualTo(new BigDecimal("100.5"));
        assertThat(Amount.of("100").toPlainBigDecimal()).isEqualTo(new BigDecimal("100"));
        assertThat(Amount.ZERO.toPlainBigDecimal()).isEqualByComparingTo("0");
    }

    @Test
    void printsPlainString() {
        assertThat(Amount.of("1.5")).hasToString("1.50000000");
        assertThat(Amount.of(1)).isNotEqualTo("1");
    }
}
