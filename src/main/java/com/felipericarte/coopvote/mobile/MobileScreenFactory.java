package com.felipericarte.coopvote.mobile;

import com.felipericarte.coopvote.agenda.Agenda;
import com.felipericarte.coopvote.mobile.api.FormItem;
import com.felipericarte.coopvote.mobile.api.FormScreen;
import com.felipericarte.coopvote.mobile.api.MobileAction;
import com.felipericarte.coopvote.mobile.api.SelectionScreen;
import com.felipericarte.coopvote.voting.VotingResult;
import com.felipericarte.coopvote.voting.VotingSessionStatus;
import com.felipericarte.coopvote.voting.VotingSummary;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Traduz o estado do domínio para o protocolo de telas descrito pelo cliente mobile.
 */
@Component
public class MobileScreenFactory {

    private final MobileCallbackUrlFactory callbackUrls;

    public MobileScreenFactory(MobileCallbackUrlFactory callbackUrls) {
        this.callbackUrls = callbackUrls;
    }

    public SelectionScreen home(List<Agenda> agendas) {
        List<MobileAction> items = new ArrayList<>();
        items.add(MobileAction.callback("Cadastrar nova pauta", callbackUrls.create("/agendas/new")));
        agendas.forEach(agenda -> items.add(MobileAction.callback(
                agenda.getTitle(), callbackUrls.create("/agendas/" + agenda.getId()))));
        return new SelectionScreen("Pautas", items);
    }

    public FormScreen newAgenda() {
        return new FormScreen(
                "Nova pauta",
                List.of(
                        FormItem.text("Informe os dados da pauta."),
                        FormItem.textInput("title", "Título"),
                        FormItem.textInput("description", "Descrição")),
                MobileAction.callback("Cadastrar", callbackUrls.create("/agendas"), Map.of()),
                MobileAction.callback("Cancelar", callbackUrls.create("/")));
    }

    public SelectionScreen agendaActions(
            Agenda agenda,
            Optional<VotingSessionStatus> sessionStatus) {
        List<MobileAction> items = new ArrayList<>();
        if (sessionStatus.isEmpty()) {
            items.add(MobileAction.callback(
                    "Abrir sessão de votação",
                    callbackUrls.create("/agendas/" + agenda.getId() + "/voting-sessions/new")));
        } else if (sessionStatus.get() == VotingSessionStatus.OPEN) {
            items.add(MobileAction.callback(
                    "Votar",
                    callbackUrls.create("/agendas/" + agenda.getId() + "/votes/new")));
            items.add(resultAction(agenda));
        } else {
            items.add(resultAction(agenda));
        }
        items.add(MobileAction.callback("Voltar às pautas", callbackUrls.create("/")));
        return new SelectionScreen(agenda.getTitle(), items);
    }

    public FormScreen newVotingSession(Agenda agenda) {
        return new FormScreen(
                "Abrir votação",
                List.of(
                        FormItem.text(agenda.getTitle()),
                        FormItem.numberInput("durationMinutes", "Duração em minutos", 1)),
                MobileAction.callback(
                        "Abrir sessão",
                        callbackUrls.create("/agendas/" + agenda.getId() + "/voting-sessions"),
                        Map.of()),
                MobileAction.callback(
                        "Cancelar", callbackUrls.create("/agendas/" + agenda.getId())));
    }

    public FormScreen vote(Agenda agenda) {
        String voteUrl = callbackUrls.create("/agendas/" + agenda.getId() + "/votes");
        return new FormScreen(
                "Registrar voto",
                List.of(
                        FormItem.text(agenda.getTitle()),
                        FormItem.textInput("associateId", "Identificador do associado")),
                MobileAction.callback("Sim", voteUrl, Map.of("vote", "Sim")),
                MobileAction.callback("Não", voteUrl, Map.of("vote", "Não")));
    }

    public FormScreen result(Agenda agenda, VotingSummary summary) {
        return new FormScreen(
                "Resultado da votação",
                List.of(
                        FormItem.text(agenda.getTitle()),
                        FormItem.text("Sessão: " + statusLabel(summary.sessionStatus())),
                        FormItem.text("Votos Sim: " + summary.yesVotes()),
                        FormItem.text("Votos Não: " + summary.noVotes()),
                        FormItem.text("Total de votos: " + summary.totalVotes()),
                        FormItem.text("Resultado: " + resultLabel(summary.result()))),
                MobileAction.callback(
                        "Voltar à pauta", callbackUrls.create("/agendas/" + agenda.getId())),
                null);
    }

    private MobileAction resultAction(Agenda agenda) {
        return MobileAction.callback(
                "Consultar resultado",
                callbackUrls.create("/agendas/" + agenda.getId() + "/result"));
    }

    private String statusLabel(VotingSessionStatus status) {
        return status == VotingSessionStatus.OPEN ? "Em andamento" : "Encerrada";
    }

    private String resultLabel(VotingResult result) {
        return switch (result) {
            case IN_PROGRESS -> "Em andamento";
            case APPROVED -> "Aprovada";
            case REJECTED -> "Rejeitada";
            case TIED -> "Empatada";
        };
    }
}
