package com.felipericarte.coopvote.mobile.api;

import java.util.List;

/**
 * Tela com conteúdo, campos de entrada e até dois botões de ação.
 */
public record FormScreen(
        String tipo,
        String titulo,
        List<FormItem> itens,
        MobileAction botaoOk,
        MobileAction botaoCancelar) implements MobileScreen {

    public FormScreen {
        if (!"FORMULARIO".equals(tipo)) {
            throw new IllegalArgumentException("tipo must be FORMULARIO");
        }
        itens = List.copyOf(itens);
    }

    public FormScreen(
            String titulo,
            List<FormItem> itens,
            MobileAction botaoOk,
            MobileAction botaoCancelar) {
        this("FORMULARIO", titulo, itens, botaoOk, botaoCancelar);
    }
}
