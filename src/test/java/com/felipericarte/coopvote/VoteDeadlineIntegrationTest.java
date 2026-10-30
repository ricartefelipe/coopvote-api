package com.felipericarte.coopvote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.felipericarte.coopvote.agenda.Agenda;
import com.felipericarte.coopvote.agenda.AgendaRepository;
import com.felipericarte.coopvote.agenda.AgendaService;
import com.felipericarte.coopvote.auth.AssociateRole;
import com.felipericarte.coopvote.eligibility.EligibilityStatus;
import com.felipericarte.coopvote.eligibility.UserInfoClient;
import com.felipericarte.coopvote.eligibility.VoterIdentity;
import com.felipericarte.coopvote.shared.error.ApiException;
import com.felipericarte.coopvote.support.IntegrationTest;
import com.felipericarte.coopvote.support.MutableClock;
import com.felipericarte.coopvote.support.TestAssociates;
import com.felipericarte.coopvote.voting.Vote;
import com.felipericarte.coopvote.voting.VoteOption;
import com.felipericarte.coopvote.voting.VoteRepository;
import com.felipericarte.coopvote.voting.VotingService;
import com.felipericarte.coopvote.voting.VotingSessionRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Fronteira temporal do voto medida com a precisão persistida (segundos) e a elegibilidade externa
 * fora da transação: o instante que vale é o da gravação, depois da consulta.
 */
@IntegrationTest
@TestPropertySource(properties = "coopvote.eligibility.enabled=true")
class VoteDeadlineIntegrationTest {

    private static final Instant OPENED_AT = Instant.parse("2026-08-28T10:00:00Z");
    private static final String VALID_CPF = "39053344705";

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

    @MockitoBean
    private UserInfoClient userInfoClient;

    private Agenda agenda;
    private Instant closesAt;

    @BeforeEach
    void openOneMinuteSession() {
        voteRepository.deleteAll();
        sessionRepository.deleteAll();
        agendaRepository.deleteAll();
        associates.clear();
        clock.set(OPENED_AT);
        agenda = agendaService.create("Prazo", null);
        closesAt = votingService.openSession(agenda.getId(), 1).getClosesAt();
        when(userInfoClient.findByCpf(anyString())).thenReturn(Optional.of(EligibilityStatus.ABLE_TO_VOTE));
    }

    @Test
    void shouldAcceptVoteOneSecondBeforeClosingAndRejectAtAndAfterClosing() {
        clock.set(closesAt.minusSeconds(1));
        Vote accepted = votingService.castVote(agenda.getId(), voter("antes"), VoteOption.YES);
        assertThat(accepted.getCastAt()).isEqualTo(closesAt.minusSeconds(1));

        clock.set(closesAt);
        assertThatThrownBy(() -> votingService.castVote(agenda.getId(), voter("no-limite"), VoteOption.YES))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo("VOTING_SESSION_CLOSED"));

        clock.set(closesAt.plusSeconds(1));
        assertThatThrownBy(() -> votingService.castVote(agenda.getId(), voter("depois"), VoteOption.YES))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo("VOTING_SESSION_CLOSED"));

        assertThat(voteRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldRejectVoteWhenEligibilityAnswersAfterTheSessionClosed() {
        clock.set(closesAt.minusSeconds(1));
        when(userInfoClient.findByCpf(VALID_CPF)).thenAnswer(invocation -> {
            clock.advance(Duration.ofSeconds(2));
            return Optional.of(EligibilityStatus.ABLE_TO_VOTE);
        });

        assertThatThrownBy(() -> votingService.castVote(agenda.getId(), voterWithCpf("lento"), VoteOption.YES))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo("VOTING_SESSION_CLOSED"));
        assertThat(voteRepository.count()).isZero();
    }

    @Test
    void shouldFailClosedWhenEligibilityProviderIsUnavailable() {
        when(userInfoClient.findByCpf(VALID_CPF)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> votingService.castVote(agenda.getId(), voterWithCpf("indisponivel"), VoteOption.YES))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getCode()).isEqualTo("ELIGIBILITY_UNAVAILABLE");
                    assertThat(e.getStatus().value()).isEqualTo(503);
                });
        assertThat(voteRepository.count()).isZero();
    }

    @Test
    void shouldCountOnlyVotesCastInsideTheSessionPeriodAndKeepFinalResultStable() {
        clock.set(closesAt.minusSeconds(1));
        votingService.castVote(agenda.getId(), voter("v1"), VoteOption.YES);
        clock.set(closesAt.plusSeconds(30));

        var first = votingService.getResult(agenda.getId(), false);
        var second = votingService.getResult(agenda.getId(), true);
        assertThat(first.sessionStatus().name()).isEqualTo("CLOSED");
        assertThat(first.yesVotes()).isEqualTo(1);
        assertThat(first).isEqualTo(second);
    }

    private VoterIdentity voter(String id) {
        associates.create(id, EnumSet.of(AssociateRole.VOTER));
        return new VoterIdentity(id, Optional.empty());
    }

    private VoterIdentity voterWithCpf(String id) {
        associates.create(id, VALID_CPF, EnumSet.of(AssociateRole.VOTER));
        return new VoterIdentity(id, Optional.of(VALID_CPF));
    }
}
