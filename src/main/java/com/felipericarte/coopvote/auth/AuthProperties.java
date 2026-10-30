package com.felipericarte.coopvote.auth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Emissão e validação dos tokens de sessão. O segredo não tem valor padrão: sem ele a aplicação
 * não sobe.
 */
@ConfigurationProperties(prefix = "coopvote.auth")
public record AuthProperties(
        String secret,
        String issuer,
        String audience,
        Duration tokenTtl,
        LocalSeed localSeed) {

    static final int MIN_SECRET_BYTES = 32;

    public AuthProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException(
                    "coopvote.auth.secret must have at least " + MIN_SECRET_BYTES + " bytes");
        }
        if (issuer == null || issuer.isBlank()) {
            issuer = "coopvote-api";
        }
        if (audience == null || audience.isBlank()) {
            audience = "coopvote-api";
        }
        if (tokenTtl == null || tokenTtl.isZero() || tokenTtl.isNegative()) {
            tokenTtl = Duration.ofMinutes(30);
        }
        if (localSeed == null) {
            localSeed = new LocalSeed(false);
        }
    }

    /** Carga sintética de associados para a jornada local; nunca para ambientes compartilhados. */
    public record LocalSeed(boolean enabled) {
    }
}
