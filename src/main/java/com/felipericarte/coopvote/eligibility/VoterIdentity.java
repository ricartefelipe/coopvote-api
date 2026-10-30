package com.felipericarte.coopvote.eligibility;

import java.util.Optional;

/**
 * Dados do eleitor autenticado necessários à elegibilidade. O CPF vem do cadastro, nunca do corpo.
 */
public record VoterIdentity(String associateId, Optional<String> cpf) {
}
