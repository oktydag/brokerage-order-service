package com.brokerage.common.domain.valueobjects;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerIdTest {

    @Test
    void trimsSurroundingWhitespace() {
        assertThat(CustomerId.of(" CUST-1 ")).isEqualTo(CustomerId.of("CUST-1"));
    }

    @Test
    void rejectsBlankIdentifiers() {
        assertThatThrownBy(() -> CustomerId.of("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CustomerId.of(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void treatsAbsentOptionalParametersAsNull() {
        assertThat(CustomerId.ofNullable(null)).isNull();
        assertThat(CustomerId.ofNullable("  ")).isNull();
        assertThat(CustomerId.ofNullable("CUST-1")).isEqualTo(CustomerId.of("CUST-1"));
    }

    @Test
    void printsItsValue() {
        assertThat(CustomerId.of("CUST-1")).hasToString("CUST-1");
    }
}
