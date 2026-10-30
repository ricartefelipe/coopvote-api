package com.felipericarte.coopvote.voting;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoteRepository extends JpaRepository<Vote, UUID> {

    boolean existsByVotingSessionIdAndAssociateId(UUID votingSessionId, String associateId);

    Optional<Vote> findByVotingSessionIdAndAssociateId(UUID votingSessionId, String associateId);

    /** Só contam votos registrados dentro do período da sessão. */
    @Query("""
            select v.option as option, count(v.id) as total
              from Vote v
             where v.votingSession.id = :sessionId
               and v.castAt >= v.votingSession.openedAt
               and v.castAt < v.votingSession.closesAt
             group by v.option
            """)
    List<VoteCount> countBySessionId(@Param("sessionId") UUID sessionId);

    interface VoteCount {
        VoteOption getOption();

        long getTotal();
    }
}
