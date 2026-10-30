package com.felipericarte.coopvote;

import static org.assertj.core.api.Assertions.assertThat;

import com.felipericarte.coopvote.agenda.Agenda;
import com.felipericarte.coopvote.agenda.AgendaRepository;
import com.felipericarte.coopvote.agenda.AgendaService;
import com.felipericarte.coopvote.support.IntegrationTest;
import com.felipericarte.coopvote.support.MutableClock;
import com.felipericarte.coopvote.voting.Vote;
import com.felipericarte.coopvote.voting.VoteOption;
import com.felipericarte.coopvote.voting.VoteRepository;
import com.felipericarte.coopvote.voting.VotingResult;
import com.felipericarte.coopvote.voting.VotingService;
import com.felipericarte.coopvote.voting.VotingSession;
import com.felipericarte.coopvote.voting.VotingSessionRepository;
import com.felipericarte.coopvote.voting.VotingSummary;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class VoteVolumeIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-28T10:00:00Z");
    private static final int VOLUME = 2_000;

    @Autowired
    private AgendaService agendaService;

    @Autowired
    private VotingService votingService;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private VotingSessionRepository sessionRepository;

    @Autowired
    private AgendaRepository agendaRepository;

    @Autowired
    private MutableClock clock;

    @BeforeEach
    void resetState() {
        voteRepository.deleteAll();
        sessionRepository.deleteAll();
        agendaRepository.deleteAll();
        clock.set(NOW);
    }

    @Test
    void shouldTallyThousandsOfVotesFromDatabaseAggregation() {
        Agenda agenda = agendaService.create("Assembleia de volume", null);
        votingService.openSession(agenda.getId(), 10);
        VotingSession session = sessionRepository.findByAgendaId(agenda.getId()).orElseThrow();

        List<Vote> votes = IntStream.rangeClosed(1, VOLUME)
                .mapToObj(index -> Vote.cast(
                        session,
                        "associate-" + index,
                        index <= 1_300 ? VoteOption.YES : VoteOption.NO,
                        NOW.plusMillis(index)))  // todos dentro do período da sessão
                .toList();
        voteRepository.saveAll(votes);
        clock.set(NOW.plus(Duration.ofMinutes(11)));

        Instant started = Instant.now();
        VotingSummary summary = votingService.getResult(agenda.getId(), true);
        Duration elapsed = Duration.between(started, Instant.now());

        assertThat(summary.yesVotes()).isEqualTo(1_300L);
        assertThat(summary.noVotes()).isEqualTo(700L);
        assertThat(summary.totalVotes()).isEqualTo((long) VOLUME);
        assertThat(summary.result()).isEqualTo(VotingResult.APPROVED);
        assertThat(elapsed.toMillis()).isLessThan(1_000);
    }
}
