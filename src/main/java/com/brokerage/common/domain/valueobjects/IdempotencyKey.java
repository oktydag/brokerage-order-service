package com.brokerage.common.domain.valueobjects;

import java.io.Serializable;

public record IdempotencyKey(String value) implements Serializable {

    public static final int MAX_LENGTH = 255;

    public IdempotencyKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
        value = value.trim();
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "idempotencyKey must be at most %d characters".formatted(MAX_LENGTH));
        }
    }

    public static IdempotencyKey of(String value) {
        return new IdempotencyKey(value);
    }

    public static IdempotencyKey ofNullable(String value) {
        return value == null || value.isBlank() ? null : new IdempotencyKey(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
