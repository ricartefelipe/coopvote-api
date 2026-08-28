package com.felipericarte.coopvote.voting;

import java.util.UUID;

/**
 * Apuração de uma sessão, calculada de forma agregada pelo banco de dados.
 */
public record VotingSummary(
        UUID agendaId,
        UUID sessionId,
        VotingSessionStatus sessionStatus,
        long yesVotes,
        long noVotes,
        VotingResult result) {

    public long totalVotes() {
        return yesVotes + noVotes;
    }
}

