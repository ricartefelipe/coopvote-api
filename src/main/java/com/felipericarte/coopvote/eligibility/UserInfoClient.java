package com.felipericarte.coopvote.eligibility;

import java.util.Optional;

@FunctionalInterface
public interface UserInfoClient {

    Optional<EligibilityStatus> findByCpf(String cpf);
}
