package com.brokerage.common.domain.valueobjects;

import com.brokerage.common.domain.InvariantViolationException;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class Amount implements Comparable<Amount>, Serializable {

    public static final int SCALE = 8;

    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    public static final Amount ZERO = new Amount(BigDecimal.ZERO);

    private final BigDecimal value;

    private Amount(BigDecimal value) {
        this.value = value.setScale(SCALE, ROUNDING);
    }

    public static Amount of(BigDecimal value) {
        Objects.requireNonNull(value, "amount must not be null");
        if (value.signum() < 0) {
            throw new IllegalArgumentException("amount must not be negative, was " + value.toPlainString());
        }
        return new Amount(value);
    }

    public static Amount of(String value) {
        return of(new BigDecimal(value));
    }

    public static Amount of(long value) {
        return of(BigDecimal.valueOf(value));
    }

    public Amount plus(Amount other) {
        return new Amount(value.add(other.value));
    }

    public Amount minus(Amount other) {
        BigDecimal result = value.subtract(other.value);
        if (result.signum() < 0) {
            throw new InvariantViolationException(
                    "amount would become negative: " + value.toPlainString() + " - " + other.value.toPlainString());
        }
        return new Amount(result);
    }

    public Amount multipliedBy(Amount other) {
        return new Amount(value.multiply(other.value));
    }

    public boolean isLessThan(Amount other) {
        return compareTo(other) < 0;
    }

    public boolean isGreaterThan(Amount other) {
        return compareTo(other) > 0;
    }

    public boolean isZero() {
        return value.signum() == 0;
    }

    public boolean isPositive() {
        return value.signum() > 0;
    }

    public BigDecimal toBigDecimal() {
        return value;
    }

    public BigDecimal toPlainBigDecimal() {
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0, ROUNDING) : stripped;
    }

    @Override
    public int compareTo(Amount other) {
        return value.compareTo(other.value);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Amount other && value.compareTo(other.value) == 0;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value.toPlainString();
    }
}
