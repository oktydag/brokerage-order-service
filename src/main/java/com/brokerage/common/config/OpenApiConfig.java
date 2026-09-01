package com.brokerage.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BASIC_AUTH = "basicAuth";

    @Bean
    OpenAPI brokerageOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Brokerage Order Service")
                        .version("1.0.0")
                        .description("Backend API for submitting, listing, cancelling "
                                + "and matching stock orders on behalf of brokerage customers."))
                .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH))
                .components(new Components().addSecuritySchemes(BASIC_AUTH,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")));
    }
}
