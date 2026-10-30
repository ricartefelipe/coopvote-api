package com.felipericarte.coopvote.eligibility;

/**
 * Decide se o eleitor autenticado pode votar. Chamada fora de transações e locks: pode envolver rede.
 */
@FunctionalInterface
public interface AssociateEligibilityPolicy {

    void assertEligible(VoterIdentity voter);
}
