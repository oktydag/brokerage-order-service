package com.brokerage.asset.web;

import com.brokerage.common.domain.valueobjects.CustomerId;
import com.brokerage.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssetApiIntegrationTest extends IntegrationTestSupport {

    @Test
    void listsEveryHoldingOfOneCustomer() throws Exception {
        CustomerId customerId = customerWith(Map.of("TRY", 100_000L, "THYAO", 500L, "ASELS", 0L));

        mockMvc.perform(get("/api/v1/assets")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .param("customerId", customerId.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].assetName").value("ASELS"));
    }

    @Test
    void filtersByAssetName() throws Exception {
        CustomerId customerId = customerWith(Map.of("TRY", 100_000L, "THYAO", 500L));

        mockMvc.perform(get("/api/v1/assets")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .param("customerId", customerId.value())
                        .param("assetName", "TRY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].size").value(100000))
                .andExpect(jsonPath("$.content[0].usableSize").value(100000))
                .andExpect(jsonPath("$.content[0].reservedSize").value(0));
    }

    @Test
    void hidesEmptyHoldingsOnRequest() throws Exception {
        CustomerId customerId = customerWith(Map.of("TRY", 100_000L, "ASELS", 0L));

        mockMvc.perform(get("/api/v1/assets")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .param("customerId", customerId.value())
                        .param("nonZeroOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].assetName").value("TRY"));
    }

    @Test
    void reportsWhatPendingOrdersHaveReserved() throws Exception {
        CustomerId customerId = customerWith(Map.of("TRY", 100_000L, "THYAO", 500L));
        mockMvc.perform(post("/api/v1/orders")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(customerId, "THYAO", "BUY", "100", "300")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/assets")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .param("customerId", customerId.value())
                        .param("assetName", "TRY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].size").value(100000))
                .andExpect(jsonPath("$.content[0].usableSize").value(70000))
                .andExpect(jsonPath("$.content[0].reservedSize").value(30000));
    }
}
