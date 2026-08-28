package com.felipericarte.coopvote.voting.api;

import com.felipericarte.coopvote.voting.Vote;
import com.felipericarte.coopvote.voting.VoteOption;
import java.time.Instant;
import java.util.UUID;

/**
 * Confirmação de que um voto foi persistido.
 */
public record VoteResponse(
        UUID id,
        UUID agendaId,
        String associateId,
        VoteOption vote,
        Instant castAt) {

    public static VoteResponse from(Vote vote, UUID agendaId) {
        return new VoteResponse(
                vote.getId(),
                agendaId,
                vote.getAssociateId(),
                vote.getOption(),
                vote.getCastAt());
    }
}

