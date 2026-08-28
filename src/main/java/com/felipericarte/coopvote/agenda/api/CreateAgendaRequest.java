package com.felipericarte.coopvote.agenda.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dados necessários para cadastrar uma pauta.
 */
public record CreateAgendaRequest(
        @Schema(example = "Construção de uma nova sede", maxLength = 200)
        @NotBlank(message = "must not be blank")
        @Size(max = 200, message = "must have at most 200 characters")
        String title,

        @Schema(example = "Deliberação sobre a proposta apresentada pelo conselho.", maxLength = 2000)
        @Size(max = 2_000, message = "must have at most 2000 characters")
        String description) {
}

