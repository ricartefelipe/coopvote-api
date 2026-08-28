package com.felipericarte.coopvote.voting;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório de votos com apuração agregada no banco de dados.
 */
public interface VoteRepository extends JpaRepository<Vote, UUID> {

    boolean existsByVotingSessionIdAndAssociateId(UUID votingSessionId, String associateId);

    @Query("""
            select v.option as option, count(v.id) as total
              from Vote v
             where v.votingSession.id = :sessionId
             group by v.option
            """)
    List<VoteCount> countBySessionId(@Param("sessionId") UUID sessionId);

    interface VoteCount {

        VoteOption getOption();

        long getTotal();
    }
}

