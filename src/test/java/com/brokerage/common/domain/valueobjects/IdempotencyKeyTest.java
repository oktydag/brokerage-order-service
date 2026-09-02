package com.brokerage.common.domain.valueobjects;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotencyKeyTest {

    @Test
    void trimsSurroundingWhitespace() {
        assertThat(IdempotencyKey.of(" K1 ")).isEqualTo(IdempotencyKey.of("K1"));
    }

    @Test
    void rejectsBlankKeys() {
        assertThatThrownBy(() -> IdempotencyKey.of(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> IdempotencyKey.of(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsKeysBeyondTheColumnWidth() {
        assertThatThrownBy(() -> IdempotencyKey.of("k".repeat(IdempotencyKey.MAX_LENGTH + 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at most");
    }

    @Test
    void treatsAbsentHeaderAsNull() {
        assertThat(IdempotencyKey.ofNullable(null)).isNull();
        assertThat(IdempotencyKey.ofNullable("")).isNull();
        assertThat(IdempotencyKey.ofNullable("K1")).isEqualTo(IdempotencyKey.of("K1"));
    }

    @Test
    void printsItsValue() {
        assertThat(IdempotencyKey.of("K1")).hasToString("K1");
    }
}
