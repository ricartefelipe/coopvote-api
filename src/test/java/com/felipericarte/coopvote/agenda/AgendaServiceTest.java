package com.felipericarte.coopvote.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.felipericarte.coopvote.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgendaServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-28T10:00:00Z");

    @Mock
    private AgendaRepository repository;

    private AgendaService service;

    @BeforeEach
    void setUp() {
        service = new AgendaService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldCreateAgendaAtCurrentTime() {
        when(repository.save(org.mockito.ArgumentMatchers.any(Agenda.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Agenda agenda = service.create("Nova sede", null);

        assertThat(agenda.getCreatedAt()).isEqualTo(NOW);
        verify(repository).save(agenda);
    }

    @Test
    void shouldReportMissingAgenda() {
        UUID agendaId = UUID.randomUUID();
        when(repository.findById(agendaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(agendaId))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("AGENDA_NOT_FOUND");
                    assertThat(exception.getStatus().value()).isEqualTo(404);
                });
    }
}

