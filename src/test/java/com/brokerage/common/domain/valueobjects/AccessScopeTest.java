package com.brokerage.common.domain.valueobjects;

import com.brokerage.common.domain.ForbiddenException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class AccessScopeTest {

    private static final CustomerId OWNER = CustomerId.of("CUST-1");
    private static final CustomerId OTHER = CustomerId.of("CUST-2");

    @Test
    void anEmployeeActsOnAnyNamedCustomer() {
        AccessScope scope = AccessScope.unrestricted();

        assertThat(scope.isUnrestricted()).isTrue();
        assertThat(scope.customerId()).isEmpty();
        assertThat(scope.resolveTarget(OTHER)).isEqualTo(OTHER);
    }

    @Test
    void anEmployeeMustNameACustomer() {
        assertThatThrownBy(() -> AccessScope.unrestricted().resolveTarget(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("customerId is required");
    }

    @Test
    void aCustomerDefaultsToTheirOwnAccount() {
        AccessScope scope = AccessScope.of(OWNER);

        assertThat(scope.isUnrestricted()).isFalse();
        assertThat(scope.customerId()).contains(OWNER);
        assertThat(scope.resolveTarget(null)).isEqualTo(OWNER);
        assertThat(scope.resolveTarget(OWNER)).isEqualTo(OWNER);
    }

    @Test
    void aCustomerCannotNameSomebodyElse() {
        assertThatThrownBy(() -> AccessScope.of(OWNER).resolveTarget(OTHER))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("CUST-2");
    }

    @Test
    void assertCoversAllowsEmployeesEverywhere() {
        assertThatCode(() -> AccessScope.unrestricted().assertCovers(OTHER)).doesNotThrowAnyException();
    }

    @Test
    void assertCoversConfinesCustomersToTheirOwnRecords() {
        assertThatCode(() -> AccessScope.of(OWNER).assertCovers(OWNER)).doesNotThrowAnyException();
        assertThatThrownBy(() -> AccessScope.of(OWNER).assertCovers(OTHER))
                .isInstanceOf(ForbiddenException.class);
    }
}
