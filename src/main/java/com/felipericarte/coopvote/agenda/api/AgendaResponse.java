package com.felipericarte.coopvote.agenda.api;

import com.felipericarte.coopvote.agenda.Agenda;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * Representação pública de uma pauta.
 */
public record AgendaResponse(
        @Schema(example = "f5bcf2b0-b24d-4e83-9e26-87b9a1a5264a") UUID id,
        String title,
        String description,
        Instant createdAt) {

    public static AgendaResponse from(Agenda agenda) {
        return new AgendaResponse(
                agenda.getId(),
                agenda.getTitle(),
                agenda.getDescription(),
                agenda.getCreatedAt());
    }
}

