package com.felipericarte.coopvote.mobile.api;

import java.util.List;

/**
 * Tela com uma lista de opções de navegação ou operação.
 */
public record SelectionScreen(
        String tipo,
        String titulo,
        List<MobileAction> itens) implements MobileScreen {

    public SelectionScreen {
        if (!"SELECAO".equals(tipo)) {
            throw new IllegalArgumentException("tipo must be SELECAO");
        }
        itens = List.copyOf(itens);
    }

    public SelectionScreen(String titulo, List<MobileAction> itens) {
        this("SELECAO", titulo, itens);
    }
}
