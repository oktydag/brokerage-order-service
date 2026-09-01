package com.brokerage.common.idempotency;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.idempotency")
public record IdempotencyProperties(Duration retention) {

    public IdempotencyProperties {
        retention = retention == null ? Duration.ofDays(1) : retention;
    }
}
