package com.brokerage.security;

import com.brokerage.common.domain.CustomerId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "username", nullable = false, unique = true, updatable = false)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 16)
    private Role role;

    @Column(name = "customer_id")
    private CustomerId customerId;

    protected AppUser() {
    }

    private AppUser(String username, String passwordHash, Role role, CustomerId customerId) {
        this.id = UUID.randomUUID();
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.customerId = customerId;
    }

    public static AppUser admin(String username, String passwordHash) {
        return new AppUser(username, passwordHash, Role.ADMIN, null);
    }

    public static AppUser customer(String username, String passwordHash, CustomerId customerId) {
        return new AppUser(username, passwordHash, Role.CUSTOMER, customerId);
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public Optional<CustomerId> getCustomerId() {
        return Optional.ofNullable(customerId);
    }
}
