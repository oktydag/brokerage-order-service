package com.brokerage.matching.web;

import com.brokerage.common.domain.valueobjects.Amount;
import com.brokerage.common.domain.valueobjects.CustomerId;
import com.brokerage.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MatchingApiIntegrationTest extends IntegrationTestSupport {

    private String place(CustomerId customerId, String side, String size, String price) throws Exception {
        return json(mockMvc.perform(post("/api/v1/orders")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(customerId, "THYAO", side, size, price)))
                .andExpect(status().isCreated())
                .andReturn())
                .get("id").asText();
    }

    private org.springframework.test.web.servlet.ResultActions match(String... orderIds) throws Exception {
        String ids = String.join("\",\"", orderIds);
        return mockMvc.perform(post("/api/v1/admin/orders/match")
                .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderIds\":[\"" + ids + "\"]}"));
    }

    @Test
    void matchingABuyMovesCashOutAndStockIn() throws Exception {
        CustomerId customerId = customerWith(Map.of("TRY", 100_000L, "THYAO", 500L));
        String orderId = place(customerId, "BUY", "100", "300");

        match(orderId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched").value(1))
                .andExpect(jsonPath("$.outcomes[0].result").value("MATCHED"));

        assertThat(sizeOf(customerId, "TRY")).isEqualTo(Amount.of(70_000));
        assertThat(usableSizeOf(customerId, "TRY")).isEqualTo(Amount.of(70_000));
        assertThat(sizeOf(customerId, "THYAO")).isEqualTo(Amount.of(600));
        assertThat(usableSizeOf(customerId, "THYAO")).isEqualTo(Amount.of(600));
    }

    @Test
    void matchingASellMovesStockOutAndCashIn() throws Exception {
        CustomerId customerId = customerWith(Map.of("TRY", 100_000L, "THYAO", 500L));
        String orderId = place(customerId, "SELL", "50", "200");

        match(orderId).andExpect(status().isOk());

        assertThat(sizeOf(customerId, "THYAO")).isEqualTo(Amount.of(450));
        assertThat(sizeOf(customerId, "TRY")).isEqualTo(Amount.of(110_000));
        assertThat(usableSizeOf(customerId, "TRY")).isEqualTo(Amount.of(110_000));
    }

    @Test
    void aRetriedBatchSettlesNothingTwice() throws Exception {
        CustomerId customerId = customerWith(Map.of("TRY", 100_000L, "THYAO", 500L));
        String orderId = place(customerId, "BUY", "100", "300");

        match(orderId).andExpect(status().isOk());
        match(orderId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched").value(0))
                .andExpect(jsonPath("$.alreadyMatched").value(1))
                .andExpect(jsonPath("$.outcomes[0].result").value("ALREADY_MATCHED"));

        assertThat(sizeOf(customerId, "TRY")).isEqualTo(Amount.of(70_000));
        assertThat(sizeOf(customerId, "THYAO")).isEqualTo(Amount.of(600));
    }

    @Test
    void oneRejectedOrderDoesNotAbortTheBatch() throws Exception {
        CustomerId customerId = customerWith(Map.of("TRY", 100_000L, "THYAO", 500L));
        String healthy = place(customerId, "BUY", "100", "300");
        String cancelled = place(customerId, "BUY", "10", "300");
        mockMvc.perform(delete("/api/v1/orders/{id}", cancelled).with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk());

        match(healthy, cancelled)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requested").value(2))
                .andExpect(jsonPath("$.matched").value(1))
                .andExpect(jsonPath("$.rejected").value(1))
                .andExpect(jsonPath("$.outcomes[1].code").value("ILLEGAL_ORDER_TRANSITION"));

        assertThat(sizeOf(customerId, "THYAO")).isEqualTo(Amount.of(600));
    }

    @Test
    void anEmptyBatchIsRejectedByValidation() throws Exception {
        mockMvc.perform(post("/api/v1/admin/orders/match")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void matchingAnUnknownOrderIsReportedNotThrown() throws Exception {
        match("00000000-0000-0000-0000-000000000000")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rejected").value(1))
                .andExpect(jsonPath("$.outcomes[0].code").value("ORDER_NOT_FOUND"));
    }
}
