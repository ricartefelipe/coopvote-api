package com.felipericarte.coopvote.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadados publicados no contrato OpenAPI.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI coopVoteOpenApi() {
        return new OpenAPI().info(new Info()
                .title("CoopVote API")
                .version("v1")
                .description("API REST para gerenciamento de pautas e votações cooperativas."));
    }
}

