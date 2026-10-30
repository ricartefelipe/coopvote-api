package com.felipericarte.coopvote.voting.api;

import com.felipericarte.coopvote.voting.VoteOption;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Escolha do associado autenticado. O eleitor é o sujeito do token; nenhum identificador no corpo
 * é aceito.
 */
public record CastVoteRequest(
        @Schema(description = "Escolha do associado", example = "Sim", allowableValues = {"Sim", "Não"})
        @NotNull(message = "must not be null")
        VoteOption vote) {
}
