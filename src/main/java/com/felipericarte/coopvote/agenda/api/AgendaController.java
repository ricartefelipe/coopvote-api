package com.felipericarte.coopvote.agenda.api;

import com.felipericarte.coopvote.agenda.Agenda;
import com.felipericarte.coopvote.agenda.AgendaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Recursos HTTP para cadastro e consulta de pautas.
 */
@RestController
@RequestMapping("/api/v1/agendas")
@Tag(name = "Agendas", description = "Cadastro e consulta das pautas")
@SecurityRequirement(name = "bearer")
public class AgendaController {

    private final AgendaService agendaService;

    public AgendaController(AgendaService agendaService) {
        this.agendaService = agendaService;
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Cadastra uma nova pauta (gestor)")
    @ApiResponse(responseCode = "403", description = "Sem papel de gestor")
    @ApiResponse(responseCode = "201", description = "Pauta cadastrada")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    public ResponseEntity<AgendaResponse> create(@Valid @RequestBody CreateAgendaRequest request) {
        Agenda agenda = agendaService.create(request.title(), request.description());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(agenda.getId())
                .toUri();
        return ResponseEntity.created(location).body(AgendaResponse.from(agenda));
    }

    @GetMapping("/{agendaId}")
    @Operation(summary = "Consulta uma pauta pelo identificador")
    @ApiResponse(responseCode = "200", description = "Pauta encontrada")
    @ApiResponse(responseCode = "404", description = "Pauta inexistente")
    public AgendaResponse getById(@PathVariable UUID agendaId) {
        return AgendaResponse.from(agendaService.getById(agendaId));
    }
}

