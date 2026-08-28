package com.felipericarte.coopvote.agenda;

import com.felipericarte.coopvote.shared.error.ApiException;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso relacionados ao ciclo de vida de pautas.
 */
@Service
public class AgendaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgendaService.class);

    private final AgendaRepository agendaRepository;
    private final Clock clock;

    public AgendaService(AgendaRepository agendaRepository, Clock clock) {
        this.agendaRepository = agendaRepository;
        this.clock = clock;
    }

    /**
     * Cadastra uma pauta pronta para receber uma futura sessão de votação.
     */
    @Transactional
    public Agenda create(String title, String description) {
        Agenda agenda = agendaRepository.save(Agenda.create(title, description, clock.instant()));
        LOGGER.atInfo()
                .addKeyValue("agendaId", agenda.getId())
                .log("Agenda created");
        return agenda;
    }

    /**
     * Obtém uma pauta ou sinaliza que o recurso não existe.
     */
    @Transactional(readOnly = true)
    public Agenda getById(UUID agendaId) {
        return agendaRepository.findById(agendaId)
                .orElseThrow(() -> ApiException.notFound(
                        "AGENDA_NOT_FOUND",
                        "Agenda '%s' was not found.".formatted(agendaId)));
    }
}

