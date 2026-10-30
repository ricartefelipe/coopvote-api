package com.felipericarte.coopvote.auth;

/**
 * Papéis da cooperativa. {@code VOTER} vota; {@code MANAGER} cadastra pautas e abre sessões;
 * {@code AUDITOR} acompanha apurações parciais sem votar nem gerir.
 */
public enum AssociateRole {
    VOTER,
    MANAGER,
    AUDITOR;

    public String authority() {
        return "ROLE_" + name();
    }
}
