package com.felipericarte.coopvote.voting.api;

import com.felipericarte.coopvote.voting.VotingResult;
import com.felipericarte.coopvote.voting.VotingSessionStatus;
import com.felipericarte.coopvote.voting.VotingSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * Contagem e resultado da votação de uma pauta. Com a sessão aberta, os totais só aparecem para
 * gestores e auditores; para os demais a resposta traz apenas o estado.
 */
public record VotingResultResponse(
        UUID agendaId,
        UUID sessionId,
        VotingSessionStatus sessionStatus,
        @Schema(description = "Ausente enquanto a sessão está aberta e o solicitante não pode ver parciais")
        Long yesVotes,
        Long noVotes,
        Long totalVotes,
        VotingResult result) {

    public static VotingResultResponse from(VotingSummary summary) {
        return new VotingResultResponse(
                summary.agendaId(),
                summary.sessionId(),
                summary.sessionStatus(),
                summary.yesVotes(),
                summary.noVotes(),
                summary.totalVotes(),
                summary.result());
    }
}
