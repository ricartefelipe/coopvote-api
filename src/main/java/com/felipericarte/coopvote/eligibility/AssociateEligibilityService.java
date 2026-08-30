package com.felipericarte.coopvote.eligibility;

import com.felipericarte.coopvote.shared.error.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
public class AssociateEligibilityService implements AssociateEligibilityPolicy {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssociateEligibilityService.class);

    private final EligibilityProperties properties;
    private final UserInfoClient userInfoClient;

    public AssociateEligibilityService(EligibilityProperties properties, UserInfoClient userInfoClient) {
        this.properties = properties;
        this.userInfoClient = userInfoClient;
    }

    @Override
    public void assertEligible(String associateId) {
        if (!properties.enabled()) {
            return;
        }

        Cpf.parse(associateId).ifPresent(this::assertCpfEligible);
    }

    private void assertCpfEligible(String cpf) {
        if (!Cpf.hasValidCheckDigits(cpf)) {
            throw ApiException.badRequest("INVALID_CPF", "O CPF informado é inválido.");
        }

        try {
            switch (userInfoClient.findByCpf(cpf).orElse(null)) {
                case ABLE_TO_VOTE -> {
                }
                case UNABLE_TO_VOTE -> throw ApiException.forbidden(
                        "ASSOCIATE_UNABLE_TO_VOTE",
                        "O associado não está habilitado a votar.");
                case NOT_FOUND -> throw ApiException.notFound(
                        "INVALID_CPF",
                        "O CPF informado não foi encontrado.");
                case null -> LOGGER.atWarn()
                        .addKeyValue("cpfSuffix", cpf.substring(9))
                        .log("Eligibility service unavailable; vote proceeds");
            }
        } catch (RestClientException exception) {
            LOGGER.atWarn()
                    .addKeyValue("cpfSuffix", cpf.substring(9))
                    .setCause(exception)
                    .log("Eligibility service unavailable; vote proceeds");
        }
    }
}
