package com.felipericarte.coopvote.auth.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Schema(example = "associate-123", maxLength = 100)
        @NotBlank(message = "must not be blank")
        @Size(max = 100, message = "must have at most 100 characters")
        String associateId,
        @Schema(example = "secret", maxLength = 200)
        @NotBlank(message = "must not be blank")
        @Size(max = 200, message = "must have at most 200 characters")
        String password) {
}
