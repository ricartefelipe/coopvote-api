package com.felipericarte.coopvote.voting.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

/**
 * Configuração opcional para abertura da sessão.
 */
public record OpenVotingSessionRequest(
        @Schema(description = "Duração em minutos; o padrão é 1 minuto", example = "5", defaultValue = "1")
        @Positive(message = "must be greater than zero")
        Integer durationMinutes) {
}

