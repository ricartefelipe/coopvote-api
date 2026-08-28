package com.felipericarte.coopvote.voting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.felipericarte.coopvote.agenda.Agenda;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class VotingSessionTest {

    private static final Instant OPENED_AT = Instant.parse("2026-08-28T10:00:00Z");
    private static final Agenda AGENDA = Agenda.create("Nova sede", null, OPENED_AT);

    @Test
    void shouldOpenSessionForTheGivenDuration() {
        var session = VotingSession.open(AGENDA, OPENED_AT, Duration.ofMinutes(5));

        assertThat(session.getId()).isNotNull();
        assertThat(session.getAgenda()).isSameAs(AGENDA);
        assertThat(session.getOpenedAt()).isEqualTo(OPENED_AT);
        assertThat(session.getClosesAt()).isEqualTo(OPENED_AT.plusSeconds(300));
    }

    @Test
    void shouldUseExclusiveClosingBoundary() {
        var session = VotingSession.open(AGENDA, OPENED_AT, Duration.ofMinutes(1));

        assertThat(session.isOpenAt(OPENED_AT)).isTrue();
        assertThat(session.isOpenAt(OPENED_AT.plusSeconds(59))).isTrue();
        assertThat(session.isOpenAt(OPENED_AT.plusSeconds(60))).isFalse();
        assertThat(session.statusAt(OPENED_AT.plusSeconds(60))).isEqualTo(VotingSessionStatus.CLOSED);
    }

    @Test
    void shouldRejectNonPositiveDuration() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> VotingSession.open(AGENDA, OPENED_AT, Duration.ZERO))
                .withMessage("duration must be positive");
    }
}

