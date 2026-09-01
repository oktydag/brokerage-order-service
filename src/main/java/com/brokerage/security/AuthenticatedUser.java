package com.brokerage.security;

import com.brokerage.common.domain.valueobjects.CustomerId;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class AuthenticatedUser implements UserDetails {

    private final String username;
    private final String passwordHash;
    private final Role role;
    private final CustomerId customerId;

    public AuthenticatedUser(AppUser user) {
        this.username = user.getUsername();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole();
        this.customerId = user.getCustomerId().orElse(null);
    }

    public Role role() {
        return role;
    }

    public Optional<CustomerId> customerId() {
        return Optional.ofNullable(customerId);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
