package com.felipericarte.coopvote.voting;

import java.util.UUID;

/**
 * Apuração de uma sessão, calculada de forma agregada pelo banco de dados. Enquanto a sessão está
 * aberta, os totais só são preenchidos para quem pode ver resultado parcial.
 */
public record VotingSummary(
        UUID agendaId,
        UUID sessionId,
        VotingSessionStatus sessionStatus,
        Long yesVotes,
        Long noVotes,
        VotingResult result) {

    public static VotingSummary inProgressWithoutCounts(UUID agendaId, UUID sessionId) {
        return new VotingSummary(agendaId, sessionId, VotingSessionStatus.OPEN, null, null, VotingResult.IN_PROGRESS);
    }

    public boolean countsVisible() {
        return yesVotes != null && noVotes != null;
    }

    public Long totalVotes() {
        return countsVisible() ? yesVotes + noVotes : null;
    }
}
