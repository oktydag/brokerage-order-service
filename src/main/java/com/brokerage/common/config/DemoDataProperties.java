package com.brokerage.common.config;

import com.brokerage.security.Role;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.List;

@ConfigurationProperties(prefix = "app.demo-data")
public record DemoDataProperties(boolean enabled, List<User> users, List<Holding> holdings) {

    public DemoDataProperties {
        users = users == null ? List.of() : users;
        holdings = holdings == null ? List.of() : holdings;
    }

    public record User(String username, String password, Role role, String customerId) {
    }

    public record Holding(String customerId, String assetName, BigDecimal size) {
    }
}
