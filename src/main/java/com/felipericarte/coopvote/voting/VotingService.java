package com.felipericarte.coopvote.voting;

import com.felipericarte.coopvote.agenda.Agenda;
import com.felipericarte.coopvote.agenda.AgendaService;
import com.felipericarte.coopvote.eligibility.AssociateEligibilityPolicy;
import com.felipericarte.coopvote.shared.error.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VotingService {

    public static final int DEFAULT_DURATION_MINUTES = 1;

    private static final Logger LOGGER = LoggerFactory.getLogger(VotingService.class);

    private final AgendaService agendaService;
    private final VotingSessionRepository sessionRepository;
    private final VoteRepository voteRepository;
    private final AssociateEligibilityPolicy eligibilityPolicy;
    private final Clock clock;

    public VotingService(
            AgendaService agendaService,
            VotingSessionRepository sessionRepository,
            VoteRepository voteRepository,
            AssociateEligibilityPolicy eligibilityPolicy,
            Clock clock) {
        this.agendaService = agendaService;
        this.sessionRepository = sessionRepository;
        this.voteRepository = voteRepository;
        this.eligibilityPolicy = eligibilityPolicy;
        this.clock = clock;
    }

    @Transactional
    public VotingSession openSession(UUID agendaId, Integer requestedDurationMinutes) {
        Agenda agenda = agendaService.getById(agendaId);
        if (sessionRepository.existsByAgendaId(agendaId)) {
            throw sessionAlreadyExists(agendaId);
        }

        int durationMinutes = Optional.ofNullable(requestedDurationMinutes)
                .orElse(DEFAULT_DURATION_MINUTES);
        VotingSession session = VotingSession.open(
                agenda,
                clock.instant(),
                Duration.ofMinutes(durationMinutes));
        try {
            VotingSession savedSession = sessionRepository.saveAndFlush(session);
            LOGGER.atInfo()
                    .addKeyValue("agendaId", agendaId)
                    .addKeyValue("sessionId", savedSession.getId())
                    .addKeyValue("durationMinutes", durationMinutes)
                    .log("Voting session opened");
            return savedSession;
        } catch (DataIntegrityViolationException exception) {
            throw sessionAlreadyExists(agendaId);
        }
    }

    @Transactional
    public Vote castVote(UUID agendaId, String associateId, VoteOption option) {
        VotingSession session = findSession(agendaId);
        Instant now = clock.instant();
        if (!session.isOpenAt(now)) {
            throw ApiException.conflict(
                    "VOTING_SESSION_CLOSED",
                    "The voting session for agenda '%s' is closed.".formatted(agendaId));
        }

        String normalizedAssociateId = associateId.trim();
        if (voteRepository.existsByVotingSessionIdAndAssociateId(session.getId(), normalizedAssociateId)) {
            throw duplicateVote(agendaId);
        }
        eligibilityPolicy.assertEligible(normalizedAssociateId);

        Vote vote = Vote.cast(session, normalizedAssociateId, option, now);
        try {
            Vote savedVote = voteRepository.saveAndFlush(vote);
            LOGGER.atDebug()
                    .addKeyValue("agendaId", agendaId)
                    .addKeyValue("sessionId", session.getId())
                    .addKeyValue("voteId", savedVote.getId())
                    .log("Vote accepted");
            return savedVote;
        } catch (DataIntegrityViolationException exception) {
            throw duplicateVote(agendaId);
        }
    }

    @Transactional(readOnly = true)
    public VotingSummary getResult(UUID agendaId) {
        VotingSession session = findSession(agendaId);
        Map<VoteOption, Long> counts = voteRepository.countBySessionId(session.getId()).stream()
                .collect(Collectors.toUnmodifiableMap(
                        VoteRepository.VoteCount::getOption,
                        VoteRepository.VoteCount::getTotal));
        long yesVotes = counts.getOrDefault(VoteOption.YES, 0L);
        long noVotes = counts.getOrDefault(VoteOption.NO, 0L);

        VotingSessionStatus status = session.statusAt(clock.instant());
        return new VotingSummary(
                agendaId,
                session.getId(),
                status,
                yesVotes,
                noVotes,
                calculateResult(status, yesVotes, noVotes));
    }

    @Transactional(readOnly = true)
    public Optional<VotingSessionStatus> findSessionStatus(UUID agendaId) {
        Instant now = clock.instant();
        return sessionRepository.findByAgendaId(agendaId)
                .map(session -> session.statusAt(now));
    }

    private VotingSession findSession(UUID agendaId) {
        return sessionRepository.findByAgendaId(agendaId)
                .orElseThrow(() -> ApiException.notFound(
                        "VOTING_SESSION_NOT_FOUND",
                        "Voting session for agenda '%s' was not found.".formatted(agendaId)));
    }

    private VotingResult calculateResult(VotingSessionStatus status, long yesVotes, long noVotes) {
        return switch (status) {
            case OPEN -> VotingResult.IN_PROGRESS;
            case CLOSED -> {
                if (yesVotes > noVotes) {
                    yield VotingResult.APPROVED;
                }
                if (noVotes > yesVotes) {
                    yield VotingResult.REJECTED;
                }
                yield VotingResult.TIED;
            }
        };
    }

    private ApiException sessionAlreadyExists(UUID agendaId) {
        return ApiException.conflict(
                "VOTING_SESSION_ALREADY_EXISTS",
                "Agenda '%s' already has a voting session.".formatted(agendaId));
    }

    private ApiException duplicateVote(UUID agendaId) {
        return ApiException.conflict(
                "DUPLICATE_VOTE",
                "The associate has already voted on agenda '%s'.".formatted(agendaId));
    }
}
