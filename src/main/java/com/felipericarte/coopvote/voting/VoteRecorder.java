package com.felipericarte.coopvote.voting;

import com.felipericarte.coopvote.shared.error.ApiException;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persistência do voto numa transação curta, sem rede. O prazo é revalidado com o relógio do
 * servidor imediatamente antes de gravar; o voto único é garantido pela constraint
 * {@code uk_vote_session_associate}, e só essa violação vira "voto duplicado".
 */
@Component
public class VoteRecorder {

    static final String UNIQUE_VOTE_CONSTRAINT = "uk_vote_session_associate";
    private static final String UNIQUE_VIOLATION_SQLSTATE = "23505";

    private final VotingSessionRepository sessionRepository;
    private final VoteRepository voteRepository;
    private final Clock clock;

    public VoteRecorder(VotingSessionRepository sessionRepository, VoteRepository voteRepository, Clock clock) {
        this.sessionRepository = sessionRepository;
        this.voteRepository = voteRepository;
        this.clock = clock;
    }

    @Transactional
    public Vote record(UUID sessionId, String associateId, VoteOption option) {
        VotingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> ApiException.notFound(
                        "VOTING_SESSION_NOT_FOUND",
                        "Não há sessão de votação para esta pauta."));
        Instant now = clock.instant();
        if (!session.isOpenAt(now)) {
            throw ApiException.conflict("VOTING_SESSION_CLOSED", "A sessão de votação já foi encerrada.");
        }
        try {
            return voteRepository.saveAndFlush(Vote.cast(session, associateId, option, now));
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateVote(exception)) {
                throw ApiException.conflict("DUPLICATE_VOTE", "Este associado já votou nesta pauta.");
            }
            throw exception;
        }
    }

    static boolean isDuplicateVote(DataIntegrityViolationException exception) {
        Throwable cause = exception.getMostSpecificCause();
        return cause instanceof SQLException sql
                && UNIQUE_VIOLATION_SQLSTATE.equals(sql.getSQLState())
                && sql.getMessage() != null
                && sql.getMessage().contains(UNIQUE_VOTE_CONSTRAINT);
    }
}
