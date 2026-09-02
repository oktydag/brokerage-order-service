package com.brokerage.order.web;

import com.brokerage.common.domain.valueobjects.Amount;
import com.brokerage.common.domain.valueobjects.CustomerId;
import com.brokerage.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IdempotencyIntegrationTest extends IntegrationTestSupport {

    private CustomerId fundedCustomer() {
        return customerWith(Map.of("TRY", 100_000L, "THYAO", 500L));
    }

    private ResultActions place(CustomerId customerId, String key, String size) throws Exception {
        var request = post("/api/v1/orders")
                .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderBody(customerId, "THYAO", "BUY", size, "300"));
        if (key != null) {
            request = request.header(OrderController.IDEMPOTENCY_KEY_HEADER, key);
        }
        return mockMvc.perform(request);
    }

    @Test
    void theFirstRequestCreatesTheOrder() throws Exception {
        CustomerId customerId = fundedCustomer();

        place(customerId, "K-" + UUID.randomUUID(), "100")
                .andExpect(status().isCreated())
                .andExpect(header().string(OrderController.IDEMPOTENCY_REPLAYED_HEADER, "false"));

        assertThat(usableSizeOf(customerId, "TRY")).isEqualTo(Amount.of(70_000));
    }

    @Test
    void aRetryReplaysTheOriginalOrderWithoutReservingAgain() throws Exception {
        CustomerId customerId = fundedCustomer();
        String key = "K-" + UUID.randomUUID();
        String firstId = json(place(customerId, key, "100").andExpect(status().isCreated()).andReturn())
                .get("id").asText();

        place(customerId, key, "100")
                .andExpect(status().isOk())
                .andExpect(header().string(OrderController.IDEMPOTENCY_REPLAYED_HEADER, "true"))
                .andExpect(jsonPath("$.id").value(firstId));

        assertThat(usableSizeOf(customerId, "TRY")).isEqualTo(Amount.of(70_000));
    }

    @Test
    void equivalentAmountsCountAsTheSameRequest() throws Exception {
        CustomerId customerId = fundedCustomer();
        String key = "K-" + UUID.randomUUID();
        place(customerId, key, "100").andExpect(status().isCreated());

        place(customerId, key, "100.00")
                .andExpect(status().isOk())
                .andExpect(header().string(OrderController.IDEMPOTENCY_REPLAYED_HEADER, "true"));
    }

    @Test
    void reusingAKeyForADifferentOrderIsRefused() throws Exception {
        CustomerId customerId = fundedCustomer();
        String key = "K-" + UUID.randomUUID();
        place(customerId, key, "100").andExpect(status().isCreated());

        place(customerId, key, "1")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSE"));

        assertThat(usableSizeOf(customerId, "TRY")).isEqualTo(Amount.of(70_000));
    }

    @Test
    void theSameKeyIsScopedPerCustomer() throws Exception {
        CustomerId first = fundedCustomer();
        CustomerId second = fundedCustomer();
        String key = "K-" + UUID.randomUUID();

        place(first, key, "100").andExpect(status().isCreated());
        place(second, key, "100").andExpect(status().isCreated());

        assertThat(usableSizeOf(first, "TRY")).isEqualTo(Amount.of(70_000));
        assertThat(usableSizeOf(second, "TRY")).isEqualTo(Amount.of(70_000));
    }

    @Test
    void withoutAKeyEveryRequestCreatesANewOrder() throws Exception {
        CustomerId customerId = fundedCustomer();

        String first = json(place(customerId, null, "100").andExpect(status().isCreated()).andReturn())
                .get("id").asText();
        String second = json(place(customerId, null, "100").andExpect(status().isCreated()).andReturn())
                .get("id").asText();

        assertThat(first).isNotEqualTo(second);
        assertThat(usableSizeOf(customerId, "TRY")).isEqualTo(Amount.of(40_000));
    }

    @Test
    void aRejectedOrderLeavesNoClaimBehind() throws Exception {
        CustomerId customerId = customerWith(Map.of("TRY", 1_000L, "THYAO", 500L));
        String key = "K-" + UUID.randomUUID();

        place(customerId, key, "100").andExpect(status().isUnprocessableEntity());

        transactions.executeWithoutResult(status -> portfolios.load(customerId));
        place(customerId, key, "100").andExpect(status().isUnprocessableEntity());
    }
}
