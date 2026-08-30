package com.felipericarte.coopvote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.felipericarte.coopvote.agenda.Agenda;
import com.felipericarte.coopvote.agenda.AgendaRepository;
import com.felipericarte.coopvote.support.MutableClock;
import com.felipericarte.coopvote.support.TestClockConfig;
import com.felipericarte.coopvote.voting.VoteRepository;
import com.felipericarte.coopvote.voting.VotingSessionRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestClockConfig.class)
class MobileScreenApiIntegrationTest {

    private static final Instant INITIAL_TIME = Instant.parse("2026-08-28T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MutableClock clock;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private VotingSessionRepository sessionRepository;

    @Autowired
    private AgendaRepository agendaRepository;

    @BeforeEach
    void resetState() {
        voteRepository.deleteAll();
        sessionRepository.deleteAll();
        agendaRepository.deleteAll();
        clock.set(INITIAL_TIME);
    }

    @Test
    void shouldExposeConfigurableCallbacksAndAnnexScreenTypes() throws Exception {
        mockMvc.perform(post("/api/v1/mobile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("SELECAO"))
                .andExpect(jsonPath("$.titulo").value("Pautas"))
                .andExpect(jsonPath("$.itens[0].texto").value("Cadastrar nova pauta"))
                .andExpect(jsonPath("$.itens[0].url")
                        .value("http://mobile.test:9090/api/v1/mobile/agendas/new"))
                .andExpect(jsonPath("$.itens[0].body").doesNotExist());

        mockMvc.perform(post("/api/v1/mobile/agendas/new"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
                .andExpect(jsonPath("$.itens[0].tipo").value("TEXTO"))
                .andExpect(jsonPath("$.itens[1].tipo").value("INPUT_TEXTO"))
                .andExpect(jsonPath("$.itens[1].id").value("title"))
                .andExpect(jsonPath("$.botaoOk.url")
                        .value("http://mobile.test:9090/api/v1/mobile/agendas"))
                .andExpect(jsonPath("$.botaoCancelar.url")
                        .value("http://mobile.test:9090/api/v1/mobile/"));
    }

    @Test
    void shouldRunCompleteVotingWorkflowThroughMobileScreens() throws Exception {
        mockMvc.perform(post("/api/v1/mobile/agendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Construção de uma nova sede",
                                  "description":"Deliberação sobre a proposta do conselho."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("SELECAO"))
                .andExpect(jsonPath("$.itens[0].texto").value("Abrir sessão de votação"));

        Agenda agenda = agendaRepository.findAll().getFirst();
        UUID agendaId = agenda.getId();

        mockMvc.perform(post("/api/v1/mobile/agendas/{agendaId}/voting-sessions/new", agendaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
                .andExpect(jsonPath("$.itens[1].tipo").value("INPUT_NUMERO"))
                .andExpect(jsonPath("$.itens[1].id").value("durationMinutes"))
                .andExpect(jsonPath("$.itens[1].valor").value(10));

        mockMvc.perform(post("/api/v1/mobile/agendas/{agendaId}/voting-sessions", agendaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"durationMinutes":5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].texto").value("Votar"))
                .andExpect(jsonPath("$.itens[1].texto").value("Consultar resultado"));

        mockMvc.perform(post("/api/v1/mobile/agendas/{agendaId}/votes/new", agendaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
                .andExpect(jsonPath("$.itens[1].id").value("associateId"))
                .andExpect(jsonPath("$.botaoOk.texto").value("Sim"))
                .andExpect(jsonPath("$.botaoOk.body.vote").value("Sim"))
                .andExpect(jsonPath("$.botaoCancelar.texto").value("Não"))
                .andExpect(jsonPath("$.botaoCancelar.body.vote").value("Não"));

        mockMvc.perform(post("/api/v1/mobile/agendas/{agendaId}/votes", agendaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"associateId":"associate-1","vote":"Sim"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("SELECAO"));

        mockMvc.perform(post("/api/v1/mobile/agendas/{agendaId}/result", agendaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
                .andExpect(jsonPath("$.itens[2].texto").value("Votos Sim: 1"))
                .andExpect(jsonPath("$.itens[3].texto").value("Votos Não: 0"))
                .andExpect(jsonPath("$.itens[5].texto").value("Resultado: Em andamento"));

        clock.advance(Duration.ofMinutes(5));

        mockMvc.perform(post("/api/v1/mobile/agendas/{agendaId}", agendaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].texto").value("Consultar resultado"))
                .andExpect(jsonPath("$.itens[1].texto").value("Voltar às pautas"));

        mockMvc.perform(post("/api/v1/mobile/agendas/{agendaId}/result", agendaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[1].texto").value("Sessão: Encerrada"))
                .andExpect(jsonPath("$.itens[5].texto").value("Resultado: Aprovada"));

        assertThat(voteRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldShowResultInsteadOfVoteWhenSessionIsClosed() throws Exception {
        mockMvc.perform(post("/api/v1/mobile/agendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Pauta de sessão curta"}
                                """))
                .andExpect(status().isOk());

        UUID agendaId = agendaRepository.findAll().getFirst().getId();

        mockMvc.perform(post("/api/v1/mobile/agendas/{agendaId}/voting-sessions", agendaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"durationMinutes":1}
                                """))
                .andExpect(status().isOk());

        clock.advance(Duration.ofMinutes(1));

        mockMvc.perform(post("/api/v1/mobile/agendas/{agendaId}/votes/new", agendaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
                .andExpect(jsonPath("$.titulo").value("Resultado da votação"))
                .andExpect(jsonPath("$.itens[1].texto").value("Sessão: Encerrada"));

        mockMvc.perform(post("/api/v1/mobile/agendas/{agendaId}/votes", agendaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"associateId":"associate-1","vote":"Sim"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
                .andExpect(jsonPath("$.titulo").value("Resultado da votação"))
                .andExpect(jsonPath("$.itens[5].texto").value("Resultado: Empatada"));

        assertThat(voteRepository.count()).isZero();
    }
}
