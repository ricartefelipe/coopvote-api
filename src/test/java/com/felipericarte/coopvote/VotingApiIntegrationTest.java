package com.felipericarte.coopvote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.felipericarte.coopvote.agenda.AgendaRepository;
import com.felipericarte.coopvote.support.IntegrationTest;
import com.felipericarte.coopvote.support.MutableClock;
import com.felipericarte.coopvote.support.TestAssociates;
import com.felipericarte.coopvote.voting.VoteRepository;
import com.felipericarte.coopvote.voting.VotingSessionRepository;
import tools.jackson.databind.JsonNode;
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

    @Autowired
    private TestAssociates associates;

    private String manager;

    @BeforeEach
    void resetState() {
        voteRepository.deleteAll();
        sessionRepository.deleteAll();
        agendaRepository.deleteAll();
        associates.clear();
        clock.set(INITIAL_TIME);
        manager = associates.manager("gestora");
    }

    @Test
    void shouldRunCompleteVotingWorkflowWithAuthenticatedVoters() throws Exception {
        UUID agendaId = createAgenda("Nova sede");
        mockMvc.perform(post("/api/v1/agendas/{agendaId}/voting-sessions", agendaId)
                        .header(HttpHeaders.AUTHORIZATION, manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"durationMinutes\":5}")
                        .header("X-Correlation-ID", "integration-test"))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Correlation-ID", "integration-test"))
                .andExpect(jsonPath("$.status").value("OPEN"));

        castVote(agendaId, associates.voter("associate-1"), "Sim").andExpect(status().isCreated())
                .andExpect(jsonPath("$.associateId").value("associate-1"));
        castVote(agendaId, associates.voter("associate-2"), "Não").andExpect(status().isCreated());
        castVote(agendaId, associates.voter("associate-3"), "Sim").andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/agendas/{agendaId}/result", agendaId).header(HttpHeaders.AUTHORIZATION, manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionStatus").value("OPEN"))
                .andExpect(jsonPath("$.yesVotes").value(2))
                .andExpect(jsonPath("$.result").value("IN_PROGRESS"));

        clock.advance(Duration.ofMinutes(6));
        mockMvc.perform(get("/api/v1/agendas/{agendaId}/result", agendaId).header(HttpHeaders.AUTHORIZATION, manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionStatus").value("CLOSED"))
                .andExpect(jsonPath("$.totalVotes").value(3))
                .andExpect(jsonPath("$.result").value("APPROVED"));

        castVote(agendaId, associates.voter("associate-4"), "Sim")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VOTING_SESSION_CLOSED"));
    }

    @Test
    void shouldBindVoteToTokenSubjectAndIgnoreIdentifiersInBody() throws Exception {
        UUID agendaId = createAgenda("Identidade");
        openSession(agendaId, 5);
        String voterA = associates.voter("associate-a");
        associates.voter("associate-b");

        mockMvc.perform(post("/api/v1/agendas/{agendaId}/votes", agendaId)
                        .header(HttpHeaders.AUTHORIZATION, voterA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"associateId\":\"associate-b\",\"vote\":\"Sim\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.associateId").value("associate-a"));

        assertThat(voteRepository.findAll()).singleElement()
                .satisfies(vote -> assertThat(vote.getAssociateId()).isEqualTo("associate-a"));

        mockMvc.perform(get("/api/v1/agendas/{agendaId}/votes/me", agendaId).header(HttpHeaders.AUTHORIZATION, voterA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.associateId").value("associate-a"))
                .andExpect(jsonPath("$.vote").value("Sim"));
    }

    @Test
    void shouldRejectDuplicateVoteFromSameAuthenticatedAssociate() throws Exception {
        UUID agendaId = createAgenda("Duplicidade");
        openSession(agendaId, 5);
        String voter = associates.voter("associate-1");
        castVote(agendaId, voter, "Sim").andExpect(status().isCreated());

        castVote(agendaId, voter, "Não")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_VOTE"));
        assertThat(voteRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldRequireAuthenticationAndRejectForgedTokens() throws Exception {
        UUID agendaId = createAgenda("Autenticação");
        openSession(agendaId, 5);

        mockMvc.perform(post("/api/v1/agendas/{agendaId}/votes", agendaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vote\":\"Sim\"}"))
                .andExpect(status().isUnauthorized());

        String forged = "Bearer eyJhbGciOiJIUzI1NiJ9."
                + "eyJpc3MiOiJjb29wdm90ZS1hcGkiLCJzdWIiOiJhc3NvY2lhdGUtMSIsInJvbGVzIjpbIlZPVEVSIl19."
                + "c2lnbmF0dXJhLWludmFsaWRh";
        mockMvc.perform(post("/api/v1/agendas/{agendaId}/votes", agendaId)
                        .header(HttpHeaders.AUTHORIZATION, forged)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vote\":\"Sim\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/agendas/{agendaId}/result", agendaId))
                .andExpect(status().isUnauthorized());
        assertThat(voteRepository.count()).isZero();
    }

    @Test
    void shouldRestrictManagementToManagersAndVotingToVoters() throws Exception {
        UUID agendaId = createAgenda("Papéis");
        String voter = associates.voter("associate-1");
        String auditor = associates.auditor("auditora");

        mockMvc.perform(post("/api/v1/agendas")
                        .header(HttpHeaders.AUTHORIZATION, voter)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Pauta do eleitor\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/agendas/{agendaId}/voting-sessions", agendaId)
                        .header(HttpHeaders.AUTHORIZATION, voter))
                .andExpect(status().isForbidden());
        assertThat(sessionRepository.count()).isZero();

        openSession(agendaId, 5);
        castVote(agendaId, auditor, "Sim").andExpect(status().isForbidden());
        assertThat(voteRepository.count()).isZero();
    }

    @Test
    void shouldHidePartialCountsFromVotersUntilSessionCloses() throws Exception {
        UUID agendaId = createAgenda("Parciais");
        openSession(agendaId, 5);
        String voter = associates.voter("associate-1");
        castVote(agendaId, voter, "Sim").andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/agendas/{agendaId}/result", agendaId).header(HttpHeaders.AUTHORIZATION, voter))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionStatus").value("OPEN"))
                .andExpect(jsonPath("$.result").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.yesVotes").doesNotExist())
                .andExpect(jsonPath("$.totalVotes").doesNotExist());

        mockMvc.perform(get("/api/v1/agendas/{agendaId}/result", agendaId)
                        .header(HttpHeaders.AUTHORIZATION, associates.auditor("auditora")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yesVotes").value(1));

        clock.advance(Duration.ofMinutes(6));
        mockMvc.perform(get("/api/v1/agendas/{agendaId}/result", agendaId).header(HttpHeaders.AUTHORIZATION, voter))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionStatus").value("CLOSED"))
                .andExpect(jsonPath("$.totalVotes").value(1))
                .andExpect(jsonPath("$.result").value("APPROVED"));
    }

    @Test
    void shouldLoginWithLocalCredentialsAndRejectWrongPassword() throws Exception {
        associates.create("associado-login", java.util.EnumSet.of(com.felipericarte.coopvote.auth.AssociateRole.VOTER));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"associateId\":\"associado-login\",\"password\":\"" + TestAssociates.PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.roles[0]").value("VOTER"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"associateId\":\"associado-login\",\"password\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"associateId\":\"inexistente\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void shouldValidateInputsAndMalformedIdentifiers() throws Exception {
        mockMvc.perform(post("/api/v1/agendas")
                        .header(HttpHeaders.AUTHORIZATION, manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/v1/agendas/not-a-uuid").header(HttpHeaders.AUTHORIZATION, manager))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidDurationAndVoteOption() throws Exception {
        UUID agendaId = createAgenda("Validação");
        mockMvc.perform(post("/api/v1/agendas/{agendaId}/voting-sessions", agendaId)
                        .header(HttpHeaders.AUTHORIZATION, manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"durationMinutes\":0}"))
                .andExpect(status().isBadRequest());

        openSession(agendaId, 5);
        castVote(agendaId, associates.voter("associate-1"), "Talvez")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"));
    }

    @Test
    void shouldReportMissingAgendaAndSessionAndSecondSession() throws Exception {
        UUID missingAgendaId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/agendas/{agendaId}/voting-sessions", missingAgendaId)
                        .header(HttpHeaders.AUTHORIZATION, manager))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("AGENDA_NOT_FOUND"));

        UUID agendaWithoutSession = createAgenda("Sem sessão");
        mockMvc.perform(get("/api/v1/agendas/{agendaId}/result", agendaWithoutSession)
                        .header(HttpHeaders.AUTHORIZATION, manager))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VOTING_SESSION_NOT_FOUND"));

        openSession(agendaWithoutSession, 1);
        mockMvc.perform(post("/api/v1/agendas/{agendaId}/voting-sessions", agendaWithoutSession)
                        .header(HttpHeaders.AUTHORIZATION, manager))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VOTING_SESSION_ALREADY_EXISTS"));
    }

    @Test
    void shouldPublishVersionedOpenApiContractWithBearerScheme() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.version").value("1.0.0"))
                .andExpect(jsonPath("$.components.securitySchemes.bearer.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/v1/auth/login']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/agendas/{agendaId}/votes/me']").exists());
    }

    private UUID createAgenda(String title) throws Exception {
        String response = mockMvc.perform(post("/api/v1/agendas")
                        .header(HttpHeaders.AUTHORIZATION, manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("title", title))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();
        JsonNode body = objectMapper.readTree(response);
        return UUID.fromString(body.get("id").asText());
    }

    private void openSession(UUID agendaId, int durationMinutes) throws Exception {
        mockMvc.perform(post("/api/v1/agendas/{agendaId}/voting-sessions", agendaId)
                        .header(HttpHeaders.AUTHORIZATION, manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"durationMinutes\":" + durationMinutes + "}"))
                .andExpect(status().isCreated());
    }

    private org.springframework.test.web.servlet.ResultActions castVote(UUID agendaId, String bearer, String vote)
            throws Exception {
        return mockMvc.perform(post("/api/v1/agendas/{agendaId}/votes", agendaId)
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"vote\":\"" + vote + "\"}"));
    }
}
