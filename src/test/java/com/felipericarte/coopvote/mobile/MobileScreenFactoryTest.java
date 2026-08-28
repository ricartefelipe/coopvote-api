package com.felipericarte.coopvote.mobile;

import static org.assertj.core.api.Assertions.assertThat;

import com.felipericarte.coopvote.agenda.Agenda;
import com.felipericarte.coopvote.mobile.api.FormScreen;
import com.felipericarte.coopvote.mobile.api.SelectionScreen;
import com.felipericarte.coopvote.voting.VotingResult;
import com.felipericarte.coopvote.voting.VotingSessionStatus;
import com.felipericarte.coopvote.voting.VotingSummary;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MobileScreenFactoryTest {

    private MobileScreenFactory screens;
    private Agenda agenda;

    @BeforeEach
    void setUp() {
        MobileCallbackUrlFactory callbackUrls = new MobileCallbackUrlFactory(
                new MobileProperties(URI.create("http://localhost:8080")));
        screens = new MobileScreenFactory(callbackUrls);
        agenda = Agenda.create("Pauta de teste", "Descrição", Instant.parse("2026-08-28T10:00:00Z"));
    }

    @Test
    void shouldOfferOnlyActionsValidForSessionState() {
        SelectionScreen withoutSession = screens.agendaActions(agenda, Optional.empty());
        SelectionScreen openSession = screens.agendaActions(
                agenda, Optional.of(VotingSessionStatus.OPEN));
        SelectionScreen closedSession = screens.agendaActions(
                agenda, Optional.of(VotingSessionStatus.CLOSED));

        assertThat(withoutSession.itens())
                .extracting(action -> action.texto())
                .containsExactly("Abrir sessão de votação", "Voltar às pautas");
        assertThat(openSession.itens())
                .extracting(action -> action.texto())
                .containsExactly("Votar", "Consultar resultado", "Voltar às pautas");
        assertThat(closedSession.itens())
                .extracting(action -> action.texto())
                .containsExactly("Consultar resultado", "Voltar às pautas");
    }

    @Test
    void shouldTranslateEveryFinalVotingResult() {
        FormScreen rejected = screens.result(
                agenda, summary(VotingResult.REJECTED, 1, 2));
        FormScreen tied = screens.result(
                agenda, summary(VotingResult.TIED, 2, 2));

        assertThat(rejected.itens().get(5).texto()).isEqualTo("Resultado: Rejeitada");
        assertThat(tied.itens().get(5).texto()).isEqualTo("Resultado: Empatada");
    }

    @Test
    void shouldPreserveActionBodiesExpectedByTheMobileProtocol() {
        FormScreen vote = screens.vote(agenda);

        assertThat(vote.tipo()).isEqualTo("FORMULARIO");
        assertThat(vote.botaoOk().body()).containsEntry("vote", "Sim");
        assertThat(vote.botaoCancelar().body()).containsEntry("vote", "Não");
    }

    private VotingSummary summary(VotingResult result, long yesVotes, long noVotes) {
        return new VotingSummary(
                agenda.getId(),
                UUID.randomUUID(),
                VotingSessionStatus.CLOSED,
                yesVotes,
                noVotes,
                result);
    }
}
