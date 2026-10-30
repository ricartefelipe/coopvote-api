package com.felipericarte.coopvote;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.felipericarte.coopvote.agenda.AgendaRepository;
import com.felipericarte.coopvote.support.IntegrationTest;
import com.felipericarte.coopvote.support.MutableClock;
import com.felipericarte.coopvote.support.TestAssociates;
import com.felipericarte.coopvote.voting.VoteRepository;
import com.felipericarte.coopvote.voting.VotingSessionRepository;
import tools.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class MobileScreenApiIntegrationTest {

    private static final Instant INITIAL_TIME = Instant.parse("2026-08-28T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MutableClock clock;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private VotingSessionRepository sessionRepository;

    @Autowired
    private AgendaRepository agendaRepository;

    @Autowired
    private TestAssociates associates;

    private String manager;
    private String voter;

    @BeforeEach
    void resetState() {
        voteRepository.deleteAll();
        sessionRepository.deleteAll();
        agendaRepository.deleteAll();
        associates.clear();
        clock.set(INITIAL_TIME);
        manager = associates.manager("gestora");
        voter = associates.voter("associate-1");
    }

    @Test
    void shouldExposeConfigurableCallbacksAndScreensAccordingToRole() throws Exception {
        mockMvc.perform(post("/api/v1/mobile").header(HttpHeaders.AUTHORIZATION, manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("SELECAO"))
                .andExpect(jsonPath("$.titulo").value("Pautas"))
                .andExpect(jsonPath("$.itens[0].texto").value("Cadastrar nova pauta"))
                .andExpect(jsonPath("$.itens[0].url").value("http://mobile.test:9090/api/v1/mobile/agendas/new"));

        mockMvc.perform(post("/api/v1/mobile").header(HttpHeaders.AUTHORIZATION, voter))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens").isEmpty());

        mockMvc.perform(post("/api/v1/mobile/agendas/new").header(HttpHeaders.AUTHORIZATION, manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("FORMULARIO"));
        mockMvc.perform(post("/api/v1/mobile/agendas/new").header(HttpHeaders.AUTHORIZATION, voter))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/mobile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRunCompleteVotingWorkflowThroughMobileScreens() throws Exception {
        String created = mockMvc.perform(post("/api/v1/mobile/agendas")
                        .header(HttpHeaders.AUTHORIZATION, manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Nova sede\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("SELECAO"))
                .andExpect(jsonPath("$.itens[0].texto").value("Abrir sessão de votação"))
                .andReturn().getResponse().getContentAsString();
        UUID agendaId = agendaIdFrom(created);

        mockMvc.perform(post("/api/v1/mobile/agendas/{agendaId}", agendaId).header(HttpHeaders.AUTHORIZATION, voter))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].texto").value("Voltar às pautas"));

        mockMvc.perform(post("/api/v1/mobile/agendas/{agendaId}/voting-sessions/new", agendaId)
                        .header(HttpHeaders.AUTHORIZATION, manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("FORMULARIO"));

        mockMvc.perform(post("/api/v1/mobile/agendas/{agendaId}/voting-sessions", agendaId)
                        .header(HttpHeaders.AUTHORIZATION, manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"durationMinutes\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].texto").value("Votar"))
                .andExpect(jsonPath("$.itens[1].texto").value("Consultar resultado"));

        mockMvc.perform(post("/api/v1/mobile/agendas/{agendaId}/votes/new", agendaId)
                        .header(HttpHeaders.AUTHORIZATION, voter))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
                .andExpect(jsonPath("$.itens[1].texto").value("O voto é registrado para o associado autenticado."))
                .andExpect(jsonPath("$.botaoOk.body.vote").value("Sim"));

        mockMvc.perform(post("/api/v1/mobile/agendas/{agendaId}/votes", agendaId)
                        .header(HttpHeaders.AUTHORIZATION, voter)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vote\":\"Sim\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("SELECAO"));

        mockMvc.perform(post("/api/v1/mobile/agendas/{agendaId}/result", agendaId).header(HttpHeaders.AUTHORIZATION, voter))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
                .andExpect(jsonPath("$.itens[2].texto").value("Votos Sim: disponível após o encerramento"))
                .andExpect(jsonPath("$.itens[5].texto").value("Resultado: Em andamento"));
        mockMvc.perform(post("/api/v1/mobile/agendas/{agendaId}/result", agendaId).header(HttpHeaders.AUTHORIZATION, manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[2].texto").value("Votos Sim: 1"))
                .andExpect(jsonPath("$.itens[3].texto").value("Votos Não: 0"));

        clock.advance(Duration.ofMinutes(6));
        mockMvc.perform(post("/api/v1/mobile/agendas/{agendaId}", agendaId).header(HttpHeaders.AUTHORIZATION, voter))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].texto").value("Consultar resultado"))
                .andExpect(jsonPath("$.itens[1].texto").value("Voltar às pautas"));

        mockMvc.perform(post("/api/v1/mobile/agendas/{agendaId}/result", agendaId).header(HttpHeaders.AUTHORIZATION, voter))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[1].texto").value("Sessão: Encerrada"))
                .andExpect(jsonPath("$.itens[2].texto").value("Votos Sim: 1"))
                .andExpect(jsonPath("$.itens[5].texto").value("Resultado: Aprovada"));
    }

    @Test
    void shouldShowResultInsteadOfVoteWhenSessionIsClosed() throws Exception {
        String created = mockMvc.perform(post("/api/v1/mobile/agendas")
                        .header(HttpHeaders.AUTHORIZATION, manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Encerrada\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        UUID agendaId = agendaIdFrom(created);
        mockMvc.perform(post("/api/v1/mobile/agendas/{agendaId}/voting-sessions", agendaId)
                        .header(HttpHeaders.AUTHORIZATION, manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"durationMinutes\":1}"))
                .andExpect(status().isOk());
        clock.advance(Duration.ofMinutes(2));

        mockMvc.perform(post("/api/v1/mobile/agendas/{agendaId}/votes/new", agendaId).header(HttpHeaders.AUTHORIZATION, voter))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Resultado da votação"))
                .andExpect(jsonPath("$.itens[1].texto").value("Sessão: Encerrada"));
        mockMvc.perform(post("/api/v1/mobile/agendas/{agendaId}/votes", agendaId)
                        .header(HttpHeaders.AUTHORIZATION, voter)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vote\":\"Sim\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Resultado da votação"));
    }

    private UUID agendaIdFrom(String selectionScreen) throws Exception {
        String url = objectMapper.readTree(selectionScreen).get("itens").get(0).get("url").asText();
        return UUID.fromString(url.replaceAll(".*/agendas/([0-9a-f-]{36}).*", "$1"));
    }
}
