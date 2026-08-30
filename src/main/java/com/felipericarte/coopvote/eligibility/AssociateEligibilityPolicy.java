package com.felipericarte.coopvote.eligibility;

@FunctionalInterface
public interface AssociateEligibilityPolicy {

    void assertEligible(String associateId);
}
