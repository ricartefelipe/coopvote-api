package com.felipericarte.coopvote.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AgendaTest {

    private static final Instant NOW = Instant.parse("2026-08-28T10:00:00Z");

    @Test
    void shouldCreateAgendaAndNormalizeText() {
        var agenda = Agenda.create("  Nova sede  ", "  Deliberação sobre a nova sede.  ", NOW);

        assertThat(agenda.getId()).isNotNull();
        assertThat(agenda.getTitle()).isEqualTo("Nova sede");
        assertThat(agenda.getDescription()).isEqualTo("Deliberação sobre a nova sede.");
        assertThat(agenda.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldNormalizeBlankDescriptionToNull() {
        var agenda = Agenda.create("Nova sede", "   ", NOW);

        assertThat(agenda.getDescription()).isNull();
    }

    @Test
    void shouldRejectBlankTitle() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Agenda.create(" ", null, NOW))
                .withMessage("title must not be blank");
    }
}

