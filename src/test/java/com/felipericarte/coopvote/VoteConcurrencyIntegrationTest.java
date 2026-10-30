package com.felipericarte.coopvote;

import static org.assertj.core.api.Assertions.assertThat;

import com.felipericarte.coopvote.agenda.Agenda;
import com.felipericarte.coopvote.agenda.AgendaRepository;
import com.felipericarte.coopvote.agenda.AgendaService;
import com.felipericarte.coopvote.eligibility.VoterIdentity;
import com.felipericarte.coopvote.shared.error.ApiException;
import com.felipericarte.coopvote.support.IntegrationTest;
import com.felipericarte.coopvote.support.MutableClock;
import com.felipericarte.coopvote.support.TestAssociates;
import com.felipericarte.coopvote.voting.VoteOption;
import com.felipericarte.coopvote.voting.VoteRepository;
import com.felipericarte.coopvote.voting.VotingService;
import com.felipericarte.coopvote.voting.VotingSessionRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class VoteConcurrencyIntegrationTest {

    private static final int ATTEMPTS = 100;

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
    private TestAssociates associates;

    @Autowired
    private MutableClock clock;

    @BeforeEach
    void resetState() {
        voteRepository.deleteAll();
        sessionRepository.deleteAll();
        agendaRepository.deleteAll();
        associates.clear();
        clock.set(Instant.parse("2026-08-28T10:00:00Z"));
    }

    @Test
    void shouldAcceptExactlyOneOfManyConcurrentVotesFromSameAssociateOnPostgres() throws Exception {
        Agenda agenda = agendaService.create("Nova sede", null);
        votingService.openSession(agenda.getId(), 5);
        associates.create("associate-1", EnumSet.of(com.felipericarte.coopvote.auth.AssociateRole.VOTER));
        VoterIdentity voter = new VoterIdentity("associate-1", Optional.empty());
        CountDownLatch start = new CountDownLatch(1);

        Callable<String> vote = () -> {
            start.await();
            try {
                votingService.castVote(agenda.getId(), voter, VoteOption.YES);
                return "accepted";
            } catch (ApiException exception) {
                return exception.getCode();
            }
        };

        List<Future<String>> futures = new ArrayList<>();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < ATTEMPTS; i++) {
                futures.add(executor.submit(vote));
            }
            start.countDown();
            List<String> outcomes = new ArrayList<>();
            for (Future<String> future : futures) {
                outcomes.add(future.get());
            }
            assertThat(outcomes).hasSize(ATTEMPTS);
            assertThat(outcomes.stream().filter("accepted"::equals).count()).isEqualTo(1);
            assertThat(outcomes.stream().collect(Collectors.toSet()))
                    .containsExactlyInAnyOrder("accepted", "DUPLICATE_VOTE");
        }
        assertThat(voteRepository.count()).isEqualTo(1);
    }
}
