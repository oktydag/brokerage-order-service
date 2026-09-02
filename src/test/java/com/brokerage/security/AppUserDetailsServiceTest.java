package com.brokerage.security;

import com.brokerage.common.domain.valueobjects.CustomerId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserDetailsServiceTest {

    @Mock
    private AppUserRepository users;

    @Test
    void loadsAKnownCredential() {
        when(users.findByUsername("alice")).thenReturn(
                Optional.of(AppUser.customer("alice", "hash", CustomerId.of("CUST-1"))));

        UserDetails details = new AppUserDetailsService(users).loadUserByUsername("alice");

        assertThat(details.getUsername()).isEqualTo("alice");
        assertThat(details).isInstanceOf(AuthenticatedUser.class);
    }

    @Test
    void refusesAnUnknownCredentialWithoutRevealingWhy() {
        when(users.findByUsername("nobody")).thenReturn(Optional.empty());
        AppUserDetailsService service = new AppUserDetailsService(users);

        assertThatThrownBy(() -> service.loadUserByUsername("nobody"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Unknown user");
    }
}
