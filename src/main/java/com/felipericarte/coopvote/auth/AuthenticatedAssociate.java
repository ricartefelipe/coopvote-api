package com.felipericarte.coopvote.auth;

import java.util.Set;

/**
 * Identidade derivada do token validado. É a única fonte do eleitor em qualquer operação.
 */
public record AuthenticatedAssociate(String associateId, Set<AssociateRole> roles) {

    public boolean has(AssociateRole role) {
        return roles.contains(role);
    }

    public boolean canManage() {
        return has(AssociateRole.MANAGER);
    }

    public boolean canSeePartialResults() {
        return has(AssociateRole.MANAGER) || has(AssociateRole.AUDITOR);
    }
}
