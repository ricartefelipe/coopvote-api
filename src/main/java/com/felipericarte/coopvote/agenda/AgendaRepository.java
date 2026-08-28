package com.felipericarte.coopvote.agenda;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório de pautas.
 */
public interface AgendaRepository extends JpaRepository<Agenda, UUID> {
}

