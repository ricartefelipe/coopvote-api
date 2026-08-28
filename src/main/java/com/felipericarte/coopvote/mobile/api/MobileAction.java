package com.felipericarte.coopvote.mobile.api;

import java.util.Map;

/**
 * Ação executada pelo cliente por meio de uma requisição POST.
 */
public record MobileAction(String texto, String url, Map<String, Object> body) {

    public MobileAction {
        body = body == null ? null : Map.copyOf(body);
    }

    public static MobileAction callback(String texto, String url) {
        return new MobileAction(texto, url, null);
    }

    public static MobileAction callback(String texto, String url, Map<String, Object> body) {
        return new MobileAction(texto, url, body);
    }
}
