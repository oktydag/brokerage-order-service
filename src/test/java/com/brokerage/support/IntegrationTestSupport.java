package com.brokerage.support;

import com.brokerage.asset.domain.Portfolio;
import com.brokerage.asset.domain.PortfolioRepository;
import com.brokerage.common.domain.valueobjects.Amount;
import com.brokerage.common.domain.valueobjects.AssetName;
import com.brokerage.common.domain.valueobjects.CustomerId;
import com.brokerage.security.AppUser;
import com.brokerage.security.AppUserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.UUID;

@SpringBootTest
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
public abstract class IntegrationTestSupport {

    public static final String ADMIN = "admin";
    public static final String ADMIN_PASSWORD = "admin123";
    public static final String CUSTOMER_PASSWORD = "secret123";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected PortfolioRepository portfolios;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlatformTransactionManager transactionManager;

    protected TransactionTemplate transactions;

    @BeforeEach
    void prepareTransactionTemplate() {
        transactions = new TransactionTemplate(transactionManager);
    }

    protected CustomerId customerWith(Map<String, Long> balances) {
        CustomerId customerId = CustomerId.of("IT-" + UUID.randomUUID());
        transactions.executeWithoutResult(status -> {
            Portfolio portfolio = Portfolio.empty(customerId);
            balances.forEach((asset, size) ->
                    portfolio.deposit(AssetName.of(asset), Amount.of(size)));
            portfolios.save(portfolio);
        });
        return customerId;
    }

    protected String loginFor(CustomerId customerId) {
        String username = "user-" + UUID.randomUUID();
        transactions.executeWithoutResult(status -> users.save(AppUser.customer(
                username, passwordEncoder.encode(CUSTOMER_PASSWORD), customerId)));
        return username;
    }

    protected Amount usableSizeOf(CustomerId customerId, String assetName) {
        return transactions.execute(status -> portfolios.load(customerId)
                .holding(AssetName.of(assetName))
                .orElseThrow()
                .getUsableSize());
    }

    protected Amount sizeOf(CustomerId customerId, String assetName) {
        return transactions.execute(status -> portfolios.load(customerId)
                .holding(AssetName.of(assetName))
                .orElseThrow()
                .getSize());
    }

    protected JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected String orderBody(String assetName, String side, String size, String price) {
        return """
                {"assetName":"%s","orderSide":"%s","size":%s,"price":%s}
                """.formatted(assetName, side, size, price);
    }

    protected String orderBody(CustomerId customerId, String assetName, String side,
                               String size, String price) {
        return """
                {"customerId":"%s","assetName":"%s","orderSide":"%s","size":%s,"price":%s}
                """.formatted(customerId.value(), assetName, side, size, price);
    }
}
