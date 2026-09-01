package com.brokerage.common.domain.valueobjects;

import java.io.Serializable;

public record CustomerId(String value) implements Serializable {

    public CustomerId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("customerId must not be blank");
        }
        value = value.trim();
    }

    public static CustomerId of(String value) {
        return new CustomerId(value);
    }

    public static CustomerId ofNullable(String value) {
        return value == null || value.isBlank() ? null : new CustomerId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
