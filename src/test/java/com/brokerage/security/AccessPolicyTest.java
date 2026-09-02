package com.brokerage.security;

import com.brokerage.common.domain.ForbiddenException;
import com.brokerage.common.domain.valueobjects.AccessScope;
import com.brokerage.common.domain.valueobjects.CustomerId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessPolicyTest {

    private final AccessPolicy policy = new AccessPolicy();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(AppUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "n/a", principal.getAuthorities()));
    }

    @Test
    void anEmployeeGetsAnUnrestrictedScope() {
        authenticate(AppUser.admin("admin", "hash"));

        AccessScope scope = policy.currentScope();

        assertThat(scope.isUnrestricted()).isTrue();
    }

    @Test
    void aCustomerIsPinnedToTheirOwnAccount() {
        authenticate(AppUser.customer("alice", "hash", CustomerId.of("CUST-1")));

        AccessScope scope = policy.currentScope();

        assertThat(scope.isUnrestricted()).isFalse();
        assertThat(scope.customerId()).contains(CustomerId.of("CUST-1"));
    }

    @Test
    void refusesAnUnauthenticatedRequest() {
        assertThatThrownBy(policy::currentScope)
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Unauthenticated");
    }

    @Test
    void refusesAPrincipalItDoesNotRecognise() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("someone", "n/a", List.of()));

        assertThatThrownBy(policy::currentScope).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void exposesTheCredentialItAuthenticated() {
        AppUser user = AppUser.customer("alice", "hash", CustomerId.of("CUST-1"));
        AuthenticatedUser principal = new AuthenticatedUser(user);

        assertThat(principal.getUsername()).isEqualTo("alice");
        assertThat(principal.getPassword()).isEqualTo("hash");
        assertThat(principal.role()).isEqualTo(Role.CUSTOMER);
        assertThat(principal.customerId()).contains(CustomerId.of("CUST-1"));
        assertThat(principal.getAuthorities()).extracting(Object::toString)
                .containsExactly("ROLE_CUSTOMER");
        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.isAccountNonLocked()).isTrue();
        assertThat(principal.isAccountNonExpired()).isTrue();
        assertThat(principal.isCredentialsNonExpired()).isTrue();
    }

    @Test
    void anAdminCredentialIsNotBoundToACustomer() {
        AuthenticatedUser principal = new AuthenticatedUser(AppUser.admin("admin", "hash"));

        assertThat(principal.customerId()).isEmpty();
        assertThat(principal.getAuthorities()).extracting(Object::toString)
                .containsExactly("ROLE_ADMIN");
    }
}
