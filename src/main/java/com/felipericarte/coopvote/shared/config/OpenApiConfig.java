package com.felipericarte.coopvote.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadados publicados no contrato OpenAPI. A versão acompanha o artefato; o esquema "bearer" é o
 * token emitido em {@code POST /api/v1/auth/login}.
 */
@Configuration
public class OpenApiConfig {

    static final String CONTRACT_VERSION = "1.0.0";

    @Bean
    OpenAPI coopVoteOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CoopVote API")
                        .version(CONTRACT_VERSION)
                        .description("API REST para gerenciamento de pautas e votações cooperativas. "
                                + "Rotas de negócio exigem o token Bearer do associado."))
                .components(new Components().addSecuritySchemes("bearer", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
