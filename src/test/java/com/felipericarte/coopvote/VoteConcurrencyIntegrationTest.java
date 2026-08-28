package com.felipericarte.coopvote;

import static org.assertj.core.api.Assertions.assertThat;

import com.felipericarte.coopvote.agenda.Agenda;
import com.felipericarte.coopvote.agenda.AgendaRepository;
import com.felipericarte.coopvote.agenda.AgendaService;
import com.felipericarte.coopvote.shared.error.ApiException;
import com.felipericarte.coopvote.support.MutableClock;
import com.felipericarte.coopvote.support.TestClockConfig;
import com.felipericarte.coopvote.voting.VoteOption;
import com.felipericarte.coopvote.voting.VoteRepository;
import com.felipericarte.coopvote.voting.VotingService;
import com.felipericarte.coopvote.voting.VotingSessionRepository;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestClockConfig.class)
class VoteConcurrencyIntegrationTest {

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
        clock.set(Instant.parse("2026-08-28T10:00:00Z"));
    }

    @Test
    void shouldAcceptOnlyOneOfTwoConcurrentVotesFromSameAssociate() throws Exception {
        Agenda agenda = agendaService.create("Nova sede", null);
        votingService.openSession(agenda.getId(), 5);
        CountDownLatch start = new CountDownLatch(1);

        Callable<String> vote = () -> {
            start.await();
            try {
                votingService.castVote(agenda.getId(), "associate-1", VoteOption.YES);
                return "accepted";
            } catch (ApiException exception) {
                return exception.getCode();
            }
        };

        var executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> first = executor.submit(vote);
            Future<String> second = executor.submit(vote);
            start.countDown();

            assertThat(List.of(first.get(), second.get()))
                    .containsExactlyInAnyOrder("accepted", "DUPLICATE_VOTE");
            assertThat(voteRepository.count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }
}

