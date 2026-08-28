package com.felipericarte.coopvote.voting;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório de sessões de votação.
 */
public interface VotingSessionRepository extends JpaRepository<VotingSession, UUID> {

    Optional<VotingSession> findByAgendaId(UUID agendaId);

    boolean existsByAgendaId(UUID agendaId);
}

