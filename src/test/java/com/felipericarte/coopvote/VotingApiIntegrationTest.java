package com.felipericarte.coopvote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class VotingApiIntegrationTest {

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

    @BeforeEach
    void resetState() {
        voteRepository.deleteAll();
        sessionRepository.deleteAll();
        agendaRepository.deleteAll();
        clock.set(INITIAL_TIME);
    }

    @Test
    void shouldRunCompleteVotingWorkflow() throws Exception {
        UUID agendaId = createAgenda();

        mockMvc.perform(post("/api/v1/agendas/{agendaId}/voting-sessions", agendaId)
                        .header("X-Correlation-ID", "integration-test"))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Correlation-ID", "integration-test"))
                .andExpect(jsonPath("$.agendaId").value(agendaId.toString()))
                .andExpect(jsonPath("$.openedAt").value("2026-08-28T10:00:00Z"))
                .andExpect(jsonPath("$.closesAt").value("2026-08-28T10:01:00Z"))
                .andExpect(jsonPath("$.status").value("OPEN"));

        castVote(agendaId, "associate-1", "Sim");
        castVote(agendaId, "associate-2", "Não");
        castVote(agendaId, "associate-3", "Sim");

        mockMvc.perform(get("/api/v1/agendas/{agendaId}/result", agendaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionStatus").value("OPEN"))
                .andExpect(jsonPath("$.yesVotes").value(2))
                .andExpect(jsonPath("$.noVotes").value(1))
                .andExpect(jsonPath("$.totalVotes").value(3))
                .andExpect(jsonPath("$.result").value("IN_PROGRESS"));

        clock.advance(Duration.ofMinutes(1));

        mockMvc.perform(get("/api/v1/agendas/{agendaId}/result", agendaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionStatus").value("CLOSED"))
                .andExpect(jsonPath("$.result").value("APPROVED"));

        mockMvc.perform(post("/api/v1/agendas/{agendaId}/votes", agendaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"associateId":"associate-4","vote":"Sim"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VOTING_SESSION_CLOSED"));
    }

    @Test
    void shouldRejectDuplicateVote() throws Exception {
        UUID agendaId = createAgenda();
        openSession(agendaId, 5);
        castVote(agendaId, "associate-1", "Sim");

        mockMvc.perform(post("/api/v1/agendas/{agendaId}/votes", agendaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"associateId":"associate-1","vote":"Não"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_VOTE"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());

        assertThat(voteRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldValidateInputsAndMalformedIdentifiers() throws Exception {
        mockMvc.perform(post("/api/v1/agendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":" ","description":"Descrição"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.title").value("must not be blank"));

        mockMvc.perform(get("/api/v1/agendas/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    void shouldRejectInvalidDurationAndVoteOption() throws Exception {
        UUID agendaId = createAgenda();

        mockMvc.perform(post("/api/v1/agendas/{agendaId}/voting-sessions", agendaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"durationMinutes":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        openSession(agendaId, 5);

        mockMvc.perform(post("/api/v1/agendas/{agendaId}/votes", agendaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"associateId":"associate-1","vote":"Talvez"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"));
    }

    @Test
    void shouldReportMissingAgendaAndSession() throws Exception {
        UUID missingAgendaId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/agendas/{agendaId}/voting-sessions", missingAgendaId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("AGENDA_NOT_FOUND"));

        UUID agendaWithoutSession = createAgenda();
        mockMvc.perform(get("/api/v1/agendas/{agendaId}/result", agendaWithoutSession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VOTING_SESSION_NOT_FOUND"));
    }

    @Test
    void shouldRejectSecondSessionForSameAgenda() throws Exception {
        UUID agendaId = createAgenda();
        openSession(agendaId, 5);

        mockMvc.perform(post("/api/v1/agendas/{agendaId}/voting-sessions", agendaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"durationMinutes":10}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VOTING_SESSION_ALREADY_EXISTS"));
    }

    @Test
    void shouldPublishVersionedOpenApiContract() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("CoopVote API"))
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.paths['/api/v1/agendas']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/mobile']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/agendas/{agendaId}/votes']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/agendas/{agendaId}/result']").exists());
    }

    private UUID createAgenda() throws Exception {
        String response = mockMvc.perform(post("/api/v1/agendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Construção de uma nova sede",
                                  "description":"Deliberação sobre a proposta do conselho."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode body = objectMapper.readTree(response);
        return UUID.fromString(body.get("id").asText());
    }

    private void openSession(UUID agendaId, int durationMinutes) throws Exception {
        mockMvc.perform(post("/api/v1/agendas/{agendaId}/voting-sessions", agendaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"durationMinutes":%d}
                                """.formatted(durationMinutes)))
                .andExpect(status().isCreated());
    }

    private void castVote(UUID agendaId, String associateId, String vote) throws Exception {
        mockMvc.perform(post("/api/v1/agendas/{agendaId}/votes", agendaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"associateId":"%s","vote":"%s"}
                                """.formatted(associateId, vote)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vote").value(vote));
    }
}
