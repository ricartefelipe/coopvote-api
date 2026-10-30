package com.felipericarte.coopvote.eligibility;

import com.felipericarte.coopvote.shared.error.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Elegibilidade pelo serviço externo quando o associado tem CPF cadastrado.
 *
 * <p>Política fail-closed: sem resposta conclusiva do provedor o voto não é aceito (503, para o
 * eleitor tentar de novo); erro técnico nunca vira elegibilidade. Só o sufixo do CPF entra no log.
 */
@Service
public class AssociateEligibilityService implements AssociateEligibilityPolicy {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssociateEligibilityService.class);
    private static final int CPF_SUFFIX_START = 9;

    private final EligibilityProperties properties;
    private final UserInfoClient userInfoClient;

    public AssociateEligibilityService(EligibilityProperties properties, UserInfoClient userInfoClient) {
        this.properties = properties;
        this.userInfoClient = userInfoClient;
    }

    @Override
    public void assertEligible(VoterIdentity voter) {
        if (!properties.enabled()) {
            return;
        }
        voter.cpf().ifPresent(this::assertCpfEligible);
    }

    private void assertCpfEligible(String cpf) {
        if (!Cpf.hasValidCheckDigits(cpf)) {
            throw ApiException.forbidden("INVALID_CPF", "O CPF cadastrado é inválido.");
        }
        switch (userInfoClient.findByCpf(cpf).orElse(null)) {
            case ABLE_TO_VOTE -> {
            }
            case UNABLE_TO_VOTE -> throw ApiException.forbidden(
                    "ASSOCIATE_UNABLE_TO_VOTE",
                    "O associado não está habilitado a votar.");
            case NOT_FOUND -> throw ApiException.forbidden(
                    "ASSOCIATE_NOT_FOUND",
                    "O associado não foi encontrado no serviço de elegibilidade.");
            case null -> {
                LOGGER.atWarn()
                        .addKeyValue("cpfSuffix", cpf.substring(CPF_SUFFIX_START))
                        .log("Eligibility service unavailable; vote refused");
                throw ApiException.serviceUnavailable(
                        "ELIGIBILITY_UNAVAILABLE",
                        "A elegibilidade não pôde ser verificada. Tente novamente.");
            }
        }
    }
}
