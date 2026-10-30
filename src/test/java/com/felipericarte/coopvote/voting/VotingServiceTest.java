package com.felipericarte.coopvote.voting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.felipericarte.coopvote.agenda.Agenda;
import com.felipericarte.coopvote.agenda.AgendaService;
import com.felipericarte.coopvote.eligibility.AssociateEligibilityPolicy;
import com.felipericarte.coopvote.eligibility.VoterIdentity;
import com.felipericarte.coopvote.shared.error.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VotingServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-28T10:00:00Z");
    private static final VoterIdentity VOTER = new VoterIdentity("associate-1", Optional.empty());

    @Mock
    private AgendaService agendaService;

    @Mock
    private VotingSessionRepository sessionRepository;

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private VoteRecorder voteRecorder;

    @Mock
    private AssociateEligibilityPolicy eligibilityPolicy;

    private VotingService service;

    @BeforeEach
    void setUp() {
        service = new VotingService(
                agendaService,
                sessionRepository,
                voteRepository,
                voteRecorder,
                eligibilityPolicy,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldUseOneMinuteAsDefaultSessionDuration() {
        Agenda agenda = Agenda.create("Nova sede", null, NOW);
        when(agendaService.getById(agenda.getId())).thenReturn(agenda);
        when(sessionRepository.saveAndFlush(any(VotingSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VotingSession session = service.openSession(agenda.getId(), null);

        assertThat(Duration.between(session.getOpenedAt(), session.getClosesAt()))
                .isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    void shouldRejectVoteWhenSessionIsClosed() {
        Agenda agenda = Agenda.create("Nova sede", null, NOW.minusSeconds(120));
        VotingSession session = VotingSession.open(agenda, NOW.minusSeconds(120), Duration.ofMinutes(1));
        when(sessionRepository.findByAgendaId(agenda.getId())).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.castVote(agenda.getId(), VOTER, VoteOption.YES))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("VOTING_SESSION_CLOSED"));
    }

    @Test
    void shouldRejectDuplicateVoteBeforeWriting() {
        Agenda agenda = Agenda.create("Nova sede", null, NOW);
        VotingSession session = VotingSession.open(agenda, NOW, Duration.ofMinutes(1));
        when(sessionRepository.findByAgendaId(agenda.getId())).thenReturn(Optional.of(session));
        when(voteRepository.existsByVotingSessionIdAndAssociateId(session.getId(), "associate-1"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.castVote(agenda.getId(), VOTER, VoteOption.YES))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("DUPLICATE_VOTE"));
    }

    @Test
    void shouldRejectIneligibleAssociate() {
        Agenda agenda = Agenda.create("Nova sede", null, NOW);
        VotingSession session = VotingSession.open(agenda, NOW, Duration.ofMinutes(1));
        when(sessionRepository.findByAgendaId(agenda.getId())).thenReturn(Optional.of(session));
        doThrow(ApiException.forbidden("ASSOCIATE_UNABLE_TO_VOTE", "blocked"))
                .when(eligibilityPolicy)
                .assertEligible(VOTER);

        assertThatThrownBy(() -> service.castVote(agenda.getId(), VOTER, VoteOption.YES))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("ASSOCIATE_UNABLE_TO_VOTE"));
    }

    @Test
    void shouldAggregateClosedVotingResult() {
        Agenda agenda = Agenda.create("Nova sede", null, NOW.minusSeconds(120));
        VotingSession session = VotingSession.open(agenda, NOW.minusSeconds(120), Duration.ofMinutes(1));
        when(sessionRepository.findByAgendaId(agenda.getId())).thenReturn(Optional.of(session));
        when(voteRepository.countBySessionId(session.getId())).thenReturn(List.of(
                count(VoteOption.YES, 3),
                count(VoteOption.NO, 1)));

        VotingSummary summary = service.getResult(agenda.getId(), true);

        assertThat(summary.yesVotes()).isEqualTo(3);
        assertThat(summary.noVotes()).isEqualTo(1);
        assertThat(summary.totalVotes()).isEqualTo(4);
        assertThat(summary.result()).isEqualTo(VotingResult.APPROVED);
        verify(voteRepository).countBySessionId(session.getId());
    }

    @ParameterizedTest
    @MethodSource("finalResults")
    void shouldCalculateEveryFinalResult(long yesVotes, long noVotes, VotingResult expectedResult) {
        Agenda agenda = Agenda.create("Nova sede", null, NOW.minusSeconds(120));
        VotingSession session = VotingSession.open(agenda, NOW.minusSeconds(120), Duration.ofMinutes(1));
        when(sessionRepository.findByAgendaId(agenda.getId())).thenReturn(Optional.of(session));
        when(voteRepository.countBySessionId(session.getId())).thenReturn(List.of(
                count(VoteOption.YES, yesVotes),
                count(VoteOption.NO, noVotes)));

        assertThat(service.getResult(agenda.getId(), true).result()).isEqualTo(expectedResult);
    }

    @Test
    void shouldHideCountsWhileOpenForWhoCannotSeePartialResults() {
        Agenda agenda = Agenda.create("Nova sede", null, NOW);
        VotingSession session = VotingSession.open(agenda, NOW, Duration.ofMinutes(1));
        when(sessionRepository.findByAgendaId(agenda.getId())).thenReturn(Optional.of(session));

        VotingSummary summary = service.getResult(agenda.getId(), false);

        assertThat(summary.countsVisible()).isFalse();
        assertThat(summary.result()).isEqualTo(VotingResult.IN_PROGRESS);
        verify(voteRepository, never()).countBySessionId(any());
    }

    @Test
    void shouldCheckEligibilityBeforeRecordingAndRecordWithTokenIdentity() {
        Agenda agenda = Agenda.create("Nova sede", null, NOW);
        VotingSession session = VotingSession.open(agenda, NOW, Duration.ofMinutes(1));
        when(sessionRepository.findByAgendaId(agenda.getId())).thenReturn(Optional.of(session));
        Vote recorded = Vote.cast(session, "associate-1", VoteOption.YES, NOW);
        when(voteRecorder.record(session.getId(), "associate-1", VoteOption.YES)).thenReturn(recorded);

        assertThat(service.castVote(agenda.getId(), VOTER, VoteOption.YES)).isSameAs(recorded);

        var order = inOrder(eligibilityPolicy, voteRecorder);
        order.verify(eligibilityPolicy).assertEligible(VOTER);
        order.verify(voteRecorder).record(session.getId(), "associate-1", VoteOption.YES);
    }

    private static Stream<Arguments> finalResults() {
        return Stream.of(
                Arguments.of(2, 1, VotingResult.APPROVED),
                Arguments.of(1, 2, VotingResult.REJECTED),
                Arguments.of(1, 1, VotingResult.TIED),
                Arguments.of(0, 0, VotingResult.TIED));
    }

    private VoteRepository.VoteCount count(VoteOption option, long total) {
        return new VoteRepository.VoteCount() {
            @Override
            public VoteOption getOption() {
                return option;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }
}
