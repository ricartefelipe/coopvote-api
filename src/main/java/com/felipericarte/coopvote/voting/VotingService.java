package com.felipericarte.coopvote.voting;

import com.felipericarte.coopvote.agenda.Agenda;
import com.felipericarte.coopvote.agenda.AgendaService;
import com.felipericarte.coopvote.shared.error.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordena abertura de sessões, recebimento de votos e apuração.
 */
@Service
public class VotingService {

    public static final int DEFAULT_DURATION_MINUTES = 1;

    private static final Logger LOGGER = LoggerFactory.getLogger(VotingService.class);

    private final AgendaService agendaService;
    private final VotingSessionRepository sessionRepository;
    private final VoteRepository voteRepository;
    private final Clock clock;

    public VotingService(
            AgendaService agendaService,
            VotingSessionRepository sessionRepository,
            VoteRepository voteRepository,
            Clock clock) {
        this.agendaService = agendaService;
        this.sessionRepository = sessionRepository;
        this.voteRepository = voteRepository;
        this.clock = clock;
    }

    /**
     * Abre a única sessão de uma pauta, usando um minuto quando a duração não é informada.
     */
    @Transactional
    public VotingSession openSession(UUID agendaId, Integer requestedDurationMinutes) {
        Agenda agenda = agendaService.getById(agendaId);
        if (sessionRepository.existsByAgendaId(agendaId)) {
            throw sessionAlreadyExists(agendaId);
        }

        int durationMinutes = requestedDurationMinutes == null
                ? DEFAULT_DURATION_MINUTES
                : requestedDurationMinutes;
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

    /**
     * Registra um voto quando a sessão está aberta e o associado ainda não participou.
     */
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

    /**
     * Retorna a contagem corrente e somente consolida o resultado após o fechamento.
     */
    @Transactional(readOnly = true)
    public VotingSummary getResult(UUID agendaId) {
        VotingSession session = findSession(agendaId);
        long yesVotes = 0;
        long noVotes = 0;
        for (VoteRepository.VoteCount count : voteRepository.countBySessionId(session.getId())) {
            if (count.getOption() == VoteOption.YES) {
                yesVotes = count.getTotal();
            } else {
                noVotes = count.getTotal();
            }
        }

        VotingSessionStatus status = session.statusAt(clock.instant());
        VotingResult result = calculateResult(status, yesVotes, noVotes);
        return new VotingSummary(agendaId, session.getId(), status, yesVotes, noVotes, result);
    }

    private VotingSession findSession(UUID agendaId) {
        return sessionRepository.findByAgendaId(agendaId)
                .orElseThrow(() -> ApiException.notFound(
                        "VOTING_SESSION_NOT_FOUND",
                        "Voting session for agenda '%s' was not found.".formatted(agendaId)));
    }

    private VotingResult calculateResult(VotingSessionStatus status, long yesVotes, long noVotes) {
        if (status == VotingSessionStatus.OPEN) {
            return VotingResult.IN_PROGRESS;
        }
        if (yesVotes > noVotes) {
            return VotingResult.APPROVED;
        }
        if (noVotes > yesVotes) {
            return VotingResult.REJECTED;
        }
        return VotingResult.TIED;
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

