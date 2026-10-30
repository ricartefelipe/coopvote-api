package com.felipericarte.coopvote.voting.api;

import com.felipericarte.coopvote.auth.AuthenticatedAssociate;
import com.felipericarte.coopvote.auth.CurrentAssociate;
import com.felipericarte.coopvote.auth.VoterIdentityResolver;
import com.felipericarte.coopvote.shared.error.ApiException;
import com.felipericarte.coopvote.voting.Vote;
import com.felipericarte.coopvote.voting.VotingService;
import com.felipericarte.coopvote.voting.VotingSession;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Clock;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recursos HTTP do processo de votação de uma pauta. O eleitor é sempre o associado autenticado.
 */
@RestController
@RequestMapping("/api/v1/agendas/{agendaId}")
@Tag(name = "Voting", description = "Abertura, participação e apuração das votações")
@SecurityRequirement(name = "bearer")
public class VotingController {

    private final VotingService votingService;
    private final CurrentAssociate currentAssociate;
    private final VoterIdentityResolver voterIdentities;
    private final Clock clock;

    public VotingController(
            VotingService votingService,
            CurrentAssociate currentAssociate,
            VoterIdentityResolver voterIdentities,
            Clock clock) {
        this.votingService = votingService;
        this.currentAssociate = currentAssociate;
        this.voterIdentities = voterIdentities;
        this.clock = clock;
    }

    @PostMapping("/voting-sessions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Abre a sessão de votação de uma pauta (gestor)")
    @ApiResponse(responseCode = "201", description = "Sessão aberta")
    @ApiResponse(responseCode = "400", description = "Duração inválida")
    @ApiResponse(responseCode = "403", description = "Sem papel de gestor")
    @ApiResponse(responseCode = "404", description = "Pauta inexistente")
    @ApiResponse(responseCode = "409", description = "A pauta já possui uma sessão")
    public VotingSessionResponse openSession(
            @PathVariable UUID agendaId,
            @Valid @RequestBody(required = false) OpenVotingSessionRequest request) {
        Integer duration = request == null ? null : request.durationMinutes();
        VotingSession session = votingService.openSession(agendaId, duration);
        return VotingSessionResponse.from(session, clock.instant());
    }

    @PostMapping("/votes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('VOTER')")
    @Operation(summary = "Registra o voto do associado autenticado")
    @ApiResponse(responseCode = "201", description = "Voto registrado")
    @ApiResponse(responseCode = "400", description = "Voto inválido")
    @ApiResponse(responseCode = "403", description = "Associado inelegível ou sem papel de eleitor")
    @ApiResponse(responseCode = "404", description = "Sessão inexistente")
    @ApiResponse(responseCode = "409", description = "Sessão fechada ou voto duplicado")
    @ApiResponse(responseCode = "503", description = "Elegibilidade indisponível; tente novamente")
    public VoteResponse castVote(
            @PathVariable UUID agendaId,
            @Valid @RequestBody CastVoteRequest request) {
        AuthenticatedAssociate voter = currentAssociate.require();
        Vote vote = votingService.castVote(agendaId, voterIdentities.resolve(voter), request.vote());
        return VoteResponse.from(vote, agendaId);
    }

    @GetMapping("/votes/me")
    @PreAuthorize("hasRole('VOTER')")
    @Operation(summary = "Recibo do voto do associado autenticado nesta pauta")
    @ApiResponse(responseCode = "200", description = "Voto encontrado")
    @ApiResponse(responseCode = "404", description = "Sessão inexistente ou associado ainda não votou")
    public VoteResponse myVote(@PathVariable UUID agendaId) {
        AuthenticatedAssociate voter = currentAssociate.require();
        return votingService.findReceipt(agendaId, voter.associateId())
                .map(vote -> VoteResponse.from(vote, agendaId))
                .orElseThrow(() -> ApiException.notFound(
                        "VOTE_NOT_FOUND",
                        "Este associado ainda não votou nesta pauta."));
    }

    @GetMapping("/result")
    @Operation(summary = "Contabiliza os votos e informa o resultado da pauta")
    @ApiResponse(responseCode = "200", description = "Apuração encontrada")
    @ApiResponse(responseCode = "404", description = "Sessão inexistente")
    public VotingResultResponse getResult(@PathVariable UUID agendaId) {
        AuthenticatedAssociate requester = currentAssociate.require();
        return VotingResultResponse.from(votingService.getResult(agendaId, requester.canSeePartialResults()));
    }
}
