package com.felipericarte.coopvote.agenda;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório de pautas.
 */
public interface AgendaRepository extends JpaRepository<Agenda, UUID> {

    List<Agenda> findAllByOrderByCreatedAtDesc();
}
