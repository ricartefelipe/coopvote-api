package com.felipericarte.coopvote.voting;

import com.felipericarte.coopvote.agenda.Agenda;
import com.felipericarte.coopvote.agenda.AgendaService;
import com.felipericarte.coopvote.eligibility.AssociateEligibilityPolicy;
import com.felipericarte.coopvote.eligibility.VoterIdentity;
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
    private final VoteRecorder voteRecorder;
    private final AssociateEligibilityPolicy eligibilityPolicy;
    private final Clock clock;

    public VotingService(
            AgendaService agendaService,
            VotingSessionRepository sessionRepository,
            VoteRepository voteRepository,
            VoteRecorder voteRecorder,
            AssociateEligibilityPolicy eligibilityPolicy,
            Clock clock) {
        this.agendaService = agendaService;
        this.sessionRepository = sessionRepository;
        this.voteRepository = voteRepository;
        this.voteRecorder = voteRecorder;
        this.eligibilityPolicy = eligibilityPolicy;
        this.clock = clock;
    }

    @Transactional
    public VotingSession openSession(UUID agendaId, Integer requestedDurationMinutes) {
        Agenda agenda = agendaService.getById(agendaId);
        if (sessionRepository.existsByAgendaId(agendaId)) {
            throw sessionAlreadyExists();
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
            throw sessionAlreadyExists();
        }
    }

    /**
     * Voto do eleitor autenticado. A elegibilidade (rede) é consultada fora de qualquer transação;
     * a gravação revalida o prazo e deixa a unicidade para o banco.
     */
    public Vote castVote(UUID agendaId, VoterIdentity voter, VoteOption option) {
        VotingSession session = findSession(agendaId);
        if (!session.isOpenAt(clock.instant())) {
            throw ApiException.conflict("VOTING_SESSION_CLOSED", "A sessão de votação já foi encerrada.");
        }
        if (voteRepository.existsByVotingSessionIdAndAssociateId(session.getId(), voter.associateId())) {
            throw ApiException.conflict("DUPLICATE_VOTE", "Este associado já votou nesta pauta.");
        }
        eligibilityPolicy.assertEligible(voter);
        Vote savedVote = voteRecorder.record(session.getId(), voter.associateId(), option);
        LOGGER.atDebug()
                .addKeyValue("agendaId", agendaId)
                .addKeyValue("sessionId", session.getId())
                .addKeyValue("voteId", savedVote.getId())
                .log("Vote accepted");
        return savedVote;
    }

    @Transactional(readOnly = true)
    public Optional<Vote> findReceipt(UUID agendaId, String associateId) {
        VotingSession session = findSession(agendaId);
        return voteRepository.findByVotingSessionIdAndAssociateId(session.getId(), associateId);
    }

    /**
     * Apuração pelo banco. Enquanto a sessão está aberta, os totais só aparecem para quem pode ver
     * resultado parcial; o resultado final é determinístico e não muda com repetições.
     */
    @Transactional(readOnly = true)
    public VotingSummary getResult(UUID agendaId, boolean partialCountsVisible) {
        VotingSession session = findSession(agendaId);
        VotingSessionStatus status = session.statusAt(clock.instant());
        if (status == VotingSessionStatus.OPEN && !partialCountsVisible) {
            return VotingSummary.inProgressWithoutCounts(agendaId, session.getId());
        }
        Map<VoteOption, Long> counts = voteRepository.countBySessionId(session.getId()).stream()
                .collect(Collectors.toUnmodifiableMap(
                        VoteRepository.VoteCount::getOption,
                        VoteRepository.VoteCount::getTotal));
        long yesVotes = counts.getOrDefault(VoteOption.YES, 0L);
        long noVotes = counts.getOrDefault(VoteOption.NO, 0L);
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
                        "Não há sessão de votação para esta pauta."));
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

    private ApiException sessionAlreadyExists() {
        return ApiException.conflict(
                "VOTING_SESSION_ALREADY_EXISTS",
                "Esta pauta já possui uma sessão de votação.");
    }
}
