package com.felipericarte.coopvote.mobile.api;

/**
 * Componente suportado por uma tela do tipo FORMULARIO.
 */
public record FormItem(String tipo, String texto, String id, String titulo, Object valor) {

    public static FormItem text(String texto) {
        return new FormItem("TEXTO", texto, null, null, null);
    }

    public static FormItem textInput(String id, String titulo) {
        return new FormItem("INPUT_TEXTO", null, id, titulo, null);
    }

    public static FormItem numberInput(String id, String titulo, Number valor) {
        return new FormItem("INPUT_NUMERO", null, id, titulo, valor);
    }
}
