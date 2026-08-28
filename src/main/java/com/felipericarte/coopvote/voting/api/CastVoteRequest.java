package com.felipericarte.coopvote.voting.api;

import com.felipericarte.coopvote.voting.VoteOption;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Identificação do associado e sua escolha na pauta.
 */
public record CastVoteRequest(
        @Schema(example = "associate-123", maxLength = 100)
        @NotBlank(message = "must not be blank")
        @Size(max = 100, message = "must have at most 100 characters")
        String associateId,

        @Schema(description = "Escolha do associado", example = "Sim", allowableValues = {"Sim", "Não"})
        @NotNull(message = "must not be null")
        VoteOption vote) {
}

