package com.felipericarte.coopvote.voting.api;

import com.felipericarte.coopvote.voting.Vote;
import com.felipericarte.coopvote.voting.VotingService;
import com.felipericarte.coopvote.voting.VotingSession;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Clock;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recursos HTTP do processo de votação de uma pauta.
 */
@RestController
@RequestMapping("/api/v1/agendas/{agendaId}")
@Tag(name = "Voting", description = "Abertura, participação e apuração das votações")
public class VotingController {

    private final VotingService votingService;
    private final Clock clock;

    public VotingController(VotingService votingService, Clock clock) {
        this.votingService = votingService;
        this.clock = clock;
    }

    @PostMapping("/voting-sessions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Abre a sessão de votação de uma pauta")
    @ApiResponse(responseCode = "201", description = "Sessão aberta")
    @ApiResponse(responseCode = "400", description = "Duração inválida")
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
    @Operation(summary = "Registra o voto de um associado")
    @ApiResponse(responseCode = "201", description = "Voto registrado")
    @ApiResponse(responseCode = "400", description = "Voto inválido")
    @ApiResponse(responseCode = "404", description = "Sessão inexistente")
    @ApiResponse(responseCode = "403", description = "Associado inelegível")
    @ApiResponse(responseCode = "409", description = "Sessão fechada ou voto duplicado")
    public VoteResponse castVote(
            @PathVariable UUID agendaId,
            @Valid @RequestBody CastVoteRequest request) {
        Vote vote = votingService.castVote(agendaId, request.associateId(), request.vote());
        return VoteResponse.from(vote, agendaId);
    }

    @GetMapping("/result")
    @Operation(summary = "Contabiliza os votos e informa o resultado da pauta")
    @ApiResponse(responseCode = "200", description = "Apuração encontrada")
    @ApiResponse(responseCode = "404", description = "Sessão inexistente")
    public VotingResultResponse getResult(@PathVariable UUID agendaId) {
        return VotingResultResponse.from(votingService.getResult(agendaId));
    }
}

