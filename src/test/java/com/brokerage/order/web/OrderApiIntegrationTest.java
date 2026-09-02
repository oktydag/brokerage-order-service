package com.brokerage.order.web;

import com.brokerage.common.domain.valueobjects.Amount;
import com.brokerage.common.domain.valueobjects.CustomerId;
import com.brokerage.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderApiIntegrationTest extends IntegrationTestSupport {

    private CustomerId fundedCustomer() {
        return customerWith(Map.of("TRY", 100_000L, "THYAO", 500L));
    }

    private MvcResult placeBuy(CustomerId customerId) throws Exception {
        return mockMvc.perform(post("/api/v1/orders")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(customerId, "THYAO", "BUY", "100", "300")))
                .andExpect(status().isCreated())
                .andReturn();
    }

    @Test
    void placingABuyReservesCashAndReturnsAPendingOrder() throws Exception {
        CustomerId customerId = fundedCustomer();

        mockMvc.perform(post("/api/v1/orders")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(customerId, "THYAO", "BUY", "100", "300")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.orderSide").value("BUY"))
                .andExpect(jsonPath("$.assetName").value("THYAO"))
                .andExpect(jsonPath("$.totalValue").value(30000));

        assertThat(usableSizeOf(customerId, "TRY")).isEqualTo(Amount.of(70_000));
        assertThat(sizeOf(customerId, "TRY")).isEqualTo(Amount.of(100_000));
    }

    @Test
    void placingASellReservesStock() throws Exception {
        CustomerId customerId = fundedCustomer();

        mockMvc.perform(post("/api/v1/orders")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(customerId, "THYAO", "SELL", "50", "200")))
                .andExpect(status().isCreated());

        assertThat(usableSizeOf(customerId, "THYAO")).isEqualTo(Amount.of(450));
        assertThat(usableSizeOf(customerId, "TRY")).isEqualTo(Amount.of(100_000));
    }

    @Test
    void anOrderThatOverdrawsTheBalanceIsRefused() throws Exception {
        CustomerId customerId = fundedCustomer();

        mockMvc.perform(post("/api/v1/orders")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(customerId, "THYAO", "BUY", "10000", "300")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_USABLE_BALANCE"))
                .andExpect(jsonPath("$.required").value(3000000))
                .andExpect(jsonPath("$.available").value(100000));

        assertThat(usableSizeOf(customerId, "TRY")).isEqualTo(Amount.of(100_000));
    }

    @Test
    void theSettlementCurrencyCannotBeTraded() throws Exception {
        CustomerId customerId = fundedCustomer();

        mockMvc.perform(post("/api/v1/orders")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(customerId, "TRY", "BUY", "10", "1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ORDER"));
    }

    @Test
    void malformedOrdersAreRejectedWithFieldLevelDetail() throws Exception {
        CustomerId customerId = fundedCustomer();

        mockMvc.perform(post("/api/v1/orders")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(customerId, "THYAO", "BUY", "-5", "300")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.violations[0].field").value("size"));
    }

    @Test
    void anEmployeeMustNameACustomer() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody("THYAO", "BUY", "1", "1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void ordersAreListedForOneCustomerWithinAHalfOpenDateRange() throws Exception {
        CustomerId customerId = fundedCustomer();
        placeBuy(customerId);

        mockMvc.perform(get("/api/v1/orders")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .param("customerId", customerId.value())
                        .param("from", "2020-01-01T00:00:00Z")
                        .param("to", "2100-01-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].customerId").value(customerId.value()));

        mockMvc.perform(get("/api/v1/orders")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .param("customerId", customerId.value())
                        .param("to", "2020-01-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void listingSupportsStatusAssetAndSideFilters() throws Exception {
        CustomerId customerId = fundedCustomer();
        placeBuy(customerId);

        mockMvc.perform(get("/api/v1/orders")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .param("customerId", customerId.value())
                        .param("status", "PENDING")
                        .param("assetName", "THYAO")
                        .param("orderSide", "BUY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/orders")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .param("customerId", customerId.value())
                        .param("status", "MATCHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void aSingleOrderCanBeFetchedById() throws Exception {
        CustomerId customerId = fundedCustomer();
        String orderId = json(placeBuy(customerId)).get("id").asText();

        mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId));

        mockMvc.perform(get("/api/v1/orders/{id}", "00000000-0000-0000-0000-000000000000")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    void cancellingAPendingOrderReleasesItsReservation() throws Exception {
        CustomerId customerId = fundedCustomer();
        String orderId = json(placeBuy(customerId)).get("id").asText();

        mockMvc.perform(delete("/api/v1/orders/{id}", orderId)
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));

        assertThat(usableSizeOf(customerId, "TRY")).isEqualTo(Amount.of(100_000));
    }

    @Test
    void aRetriedCancellationConvergesInsteadOfFailing() throws Exception {
        CustomerId customerId = fundedCustomer();
        String orderId = json(placeBuy(customerId)).get("id").asText();

        mockMvc.perform(delete("/api/v1/orders/{id}", orderId).with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/orders/{id}", orderId).with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));

        assertThat(usableSizeOf(customerId, "TRY")).isEqualTo(Amount.of(100_000));
    }

    @Test
    void cancellingAnUnknownOrderIsNotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/orders/{id}", "00000000-0000-0000-0000-000000000000")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isNotFound());
    }
}
