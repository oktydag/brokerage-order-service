package com.brokerage.common.config;

import com.brokerage.asset.domain.Portfolio;
import com.brokerage.asset.domain.PortfolioRepository;
import com.brokerage.common.domain.valueobjects.Amount;
import com.brokerage.common.domain.valueobjects.AssetName;
import com.brokerage.common.domain.valueobjects.CustomerId;
import com.brokerage.security.AppUser;
import com.brokerage.security.AppUserRepository;
import com.brokerage.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "app.demo-data", name = "enabled", havingValue = "true")
public class DemoDataSeeder implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final DemoDataProperties properties;
    private final AppUserRepository users;
    private final PortfolioRepository portfolios;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;

    public DemoDataSeeder(DemoDataProperties properties, AppUserRepository users,
                          PortfolioRepository portfolios, PasswordEncoder passwordEncoder,
                          PlatformTransactionManager transactionManager) {
        this.properties = properties;
        this.users = users;
        this.portfolios = portfolios;
        this.passwordEncoder = passwordEncoder;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void afterPropertiesSet() {
        transactionTemplate.executeWithoutResult(status -> {
            if (users.count() > 0) {
                return;
            }
            seedUsers();
            seedHoldings();
            log.info("Seeded {} demo users and {} demo holdings",
                    properties.users().size(), properties.holdings().size());
        });
    }

    private void seedUsers() {
        properties.users().stream()
                .map(this::toAppUser)
                .forEach(users::save);
    }

    private AppUser toAppUser(DemoDataProperties.User user) {
        String hash = passwordEncoder.encode(user.password());
        return user.role() == Role.ADMIN
                ? AppUser.admin(user.username(), hash)
                : AppUser.customer(user.username(), hash, CustomerId.of(user.customerId()));
    }

    private void seedHoldings() {
        Map<CustomerId, List<DemoDataProperties.Holding>> byCustomer = new LinkedHashMap<>();
        for (DemoDataProperties.Holding holding : properties.holdings()) {
            byCustomer.computeIfAbsent(CustomerId.of(holding.customerId()), key -> new ArrayList<>())
                    .add(holding);
        }
        byCustomer.forEach((customerId, holdings) -> {
            Portfolio portfolio = Portfolio.empty(customerId);
            holdings.forEach(holding -> portfolio.deposit(
                    AssetName.of(holding.assetName()), Amount.of(holding.size())));
            portfolios.save(portfolio);
        });
    }
}
