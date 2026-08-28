package com.felipericarte.coopvote.voting.api;

import com.felipericarte.coopvote.voting.VotingSession;
import com.felipericarte.coopvote.voting.VotingSessionStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Representação da janela de votação aberta para uma pauta.
 */
public record VotingSessionResponse(
        UUID id,
        UUID agendaId,
        Instant openedAt,
        Instant closesAt,
        VotingSessionStatus status) {

    public static VotingSessionResponse from(VotingSession session, Instant currentTime) {
        return new VotingSessionResponse(
                session.getId(),
                session.getAgenda().getId(),
                session.getOpenedAt(),
                session.getClosesAt(),
                session.statusAt(currentTime));
    }
}

