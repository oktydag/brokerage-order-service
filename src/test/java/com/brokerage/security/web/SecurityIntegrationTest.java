package com.brokerage.security.web;

import com.brokerage.common.domain.valueobjects.CustomerId;
import com.brokerage.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityIntegrationTest extends IntegrationTestSupport {

    @Test
    void anonymousRequestsAreChallenged() throws Exception {
        mockMvc.perform(get("/api/v1/assets"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"brokerage\""))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void badCredentialsAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/assets").with(httpBasic(ADMIN, "wrong")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void theApiDocumentationIsPublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void aCustomerSeesOnlyTheirOwnHoldings() throws Exception {
        CustomerId owner = customerWith(Map.of("TRY", 10_000L));
        CustomerId other = customerWith(Map.of("TRY", 10_000L));
        String username = loginFor(owner);

        mockMvc.perform(get("/api/v1/assets").with(httpBasic(username, CUSTOMER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].customerId").value(owner.value()));

        mockMvc.perform(get("/api/v1/assets")
                        .with(httpBasic(username, CUSTOMER_PASSWORD))
                        .param("customerId", other.value()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void aCustomerCannotPlaceOrdersForSomebodyElse() throws Exception {
        CustomerId owner = customerWith(Map.of("TRY", 100_000L, "THYAO", 100L));
        CustomerId other = customerWith(Map.of("TRY", 100_000L, "THYAO", 100L));
        String username = loginFor(owner);

        mockMvc.perform(post("/api/v1/orders")
                        .with(httpBasic(username, CUSTOMER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(other, "THYAO", "BUY", "1", "300")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/orders")
                        .with(httpBasic(username, CUSTOMER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody("THYAO", "BUY", "1", "300")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(owner.value()));
    }

    @Test
    void aCustomerCannotReachAnotherCustomersOrder() throws Exception {
        CustomerId owner = customerWith(Map.of("TRY", 100_000L, "THYAO", 100L));
        CustomerId other = customerWith(Map.of("TRY", 100_000L, "THYAO", 100L));
        String username = loginFor(owner);
        String foreignOrder = json(mockMvc.perform(post("/api/v1/orders")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(other, "THYAO", "BUY", "1", "300")))
                .andExpect(status().isCreated())
                .andReturn())
                .get("id").asText();

        mockMvc.perform(get("/api/v1/orders/{id}", foreignOrder)
                        .with(httpBasic(username, CUSTOMER_PASSWORD)))
                .andExpect(status().isForbidden());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/orders/{id}", foreignOrder)
                        .with(httpBasic(username, CUSTOMER_PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void matchingIsRestrictedToEmployees() throws Exception {
        CustomerId owner = customerWith(Map.of("TRY", 10_000L));
        String username = loginFor(owner);

        mockMvc.perform(post("/api/v1/admin/orders/match")
                        .with(httpBasic(username, CUSTOMER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderIds\":[\"00000000-0000-0000-0000-000000000000\"]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/v1/admin/orders/match")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderIds\":[\"00000000-0000-0000-0000-000000000000\"]}"))
                .andExpect(status().isOk());
    }
}
