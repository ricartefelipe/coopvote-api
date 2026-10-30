package com.felipericarte.coopvote.mobile.api;

import com.felipericarte.coopvote.agenda.Agenda;
import com.felipericarte.coopvote.agenda.AgendaService;
import com.felipericarte.coopvote.agenda.api.CreateAgendaRequest;
import com.felipericarte.coopvote.auth.AuthenticatedAssociate;
import com.felipericarte.coopvote.auth.CurrentAssociate;
import com.felipericarte.coopvote.auth.VoterIdentityResolver;
import com.felipericarte.coopvote.mobile.MobileScreenFactory;
import com.felipericarte.coopvote.voting.VotingService;
import com.felipericarte.coopvote.voting.VotingSessionStatus;
import com.felipericarte.coopvote.voting.api.CastVoteRequest;
import com.felipericarte.coopvote.voting.api.OpenVotingSessionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fluxo server-driven UI consumido pelo aplicativo mobile. Toda tela exige o token do associado;
 * as ações oferecidas seguem o papel de quem pede.
 */
@RestController
@RequestMapping("/api/v1/mobile")
@Tag(name = "Mobile Screens", description = "Mensagens FORMULARIO e SELECAO para o cliente mobile")
@SecurityRequirement(name = "bearer")
public class MobileScreenController {

    private final AgendaService agendaService;
    private final VotingService votingService;
    private final MobileScreenFactory screens;
    private final CurrentAssociate currentAssociate;
    private final VoterIdentityResolver voterIdentities;

    public MobileScreenController(
            AgendaService agendaService,
            VotingService votingService,
            MobileScreenFactory screens,
            CurrentAssociate currentAssociate,
            VoterIdentityResolver voterIdentities) {
        this.agendaService = agendaService;
        this.votingService = votingService;
        this.screens = screens;
        this.currentAssociate = currentAssociate;
        this.voterIdentities = voterIdentities;
    }

    @PostMapping({"", "/"})
    @Operation(summary = "Abre a lista inicial de pautas")
    public SelectionScreen home() {
        return screens.home(agendaService.listAll(), currentAssociate.require().canManage());
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/agendas/new")
    @Operation(summary = "Monta o formulário de cadastro de pauta (gestor)")
    public FormScreen newAgenda() {
        return screens.newAgenda();
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/agendas")
    @Operation(summary = "Cadastra uma pauta e apresenta suas ações (gestor)")
    public SelectionScreen createAgenda(@Valid @RequestBody CreateAgendaRequest request) {
        Agenda agenda = agendaService.create(request.title(), request.description());
        return screens.agendaActions(agenda, Optional.empty(), true);
    }

    @PostMapping("/agendas/{agendaId}")
    @Operation(summary = "Apresenta as ações disponíveis para uma pauta")
    public SelectionScreen agendaActions(@PathVariable UUID agendaId) {
        Agenda agenda = agendaService.getById(agendaId);
        return screens.agendaActions(
                agenda, votingService.findSessionStatus(agendaId), currentAssociate.require().canManage());
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/agendas/{agendaId}/voting-sessions/new")
    @Operation(summary = "Monta o formulário de abertura da sessão (gestor)")
    public FormScreen newVotingSession(@PathVariable UUID agendaId) {
        return screens.newVotingSession(agendaService.getById(agendaId));
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/agendas/{agendaId}/voting-sessions")
    @Operation(summary = "Abre a sessão e apresenta as ações da pauta (gestor)")
    public SelectionScreen openVotingSession(
            @PathVariable UUID agendaId,
            @Valid @RequestBody(required = false) OpenVotingSessionRequest request) {
        Agenda agenda = agendaService.getById(agendaId);
        Integer durationMinutes = request == null ? null : request.durationMinutes();
        votingService.openSession(agendaId, durationMinutes);
        return screens.agendaActions(agenda, Optional.of(VotingSessionStatus.OPEN), true);
    }

    @PreAuthorize("hasRole('VOTER')")
    @PostMapping("/agendas/{agendaId}/votes/new")
    @Operation(summary = "Monta o formulário de votação")
    public MobileScreen newVote(@PathVariable UUID agendaId) {
        Agenda agenda = agendaService.getById(agendaId);
        return screenIfVotingUnavailable(agenda).orElseGet(() -> screens.vote(agenda));
    }

    @PreAuthorize("hasRole('VOTER')")
    @PostMapping("/agendas/{agendaId}/votes")
    @Operation(summary = "Registra o voto do associado autenticado e reapresenta as ações da pauta")
    public MobileScreen castVote(
            @PathVariable UUID agendaId,
            @Valid @RequestBody CastVoteRequest request) {
        Agenda agenda = agendaService.getById(agendaId);
        AuthenticatedAssociate voter = currentAssociate.require();
        return screenIfVotingUnavailable(agenda).orElseGet(() -> {
            votingService.castVote(agendaId, voterIdentities.resolve(voter), request.vote());
            return screens.agendaActions(agenda, votingService.findSessionStatus(agendaId), voter.canManage());
        });
    }

    @PostMapping("/agendas/{agendaId}/result")
    @Operation(summary = "Apresenta a apuração da pauta")
    public FormScreen result(@PathVariable UUID agendaId) {
        Agenda agenda = agendaService.getById(agendaId);
        return screens.result(
                agenda, votingService.getResult(agendaId, currentAssociate.require().canSeePartialResults()));
    }

    private Optional<MobileScreen> screenIfVotingUnavailable(Agenda agenda) {
        AuthenticatedAssociate requester = currentAssociate.require();
        Optional<VotingSessionStatus> status = votingService.findSessionStatus(agenda.getId());
        return switch (status.orElse(null)) {
            case null -> Optional.of(screens.agendaActions(agenda, status, requester.canManage()));
            case CLOSED -> Optional.of(screens.result(
                    agenda, votingService.getResult(agenda.getId(), requester.canSeePartialResults())));
            case OPEN -> Optional.empty();
        };
    }
}
