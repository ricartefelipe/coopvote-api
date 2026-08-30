package com.felipericarte.coopvote.eligibility;

import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpUserInfoClient implements UserInfoClient {

    private final RestClient restClient;

    public HttpUserInfoClient(RestClient eligibilityRestClient) {
        this.restClient = eligibilityRestClient;
    }

    @Override
    public Optional<EligibilityStatus> findByCpf(String cpf) {
        try {
            UserInfoResponse response = restClient.get()
                    .uri("/users/{cpf}", cpf)
                    .retrieve()
                    .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (request, httpResponse) -> {
                        throw new CpfLookupNotFound();
                    })
                    .body(UserInfoResponse.class);
            return Optional.ofNullable(response)
                    .map(UserInfoResponse::status)
                    .flatMap(EligibilityStatus::fromExternal);
        } catch (CpfLookupNotFound exception) {
            return Optional.of(EligibilityStatus.NOT_FOUND);
        } catch (RestClientException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private record UserInfoResponse(String status) {
    }

    private static final class CpfLookupNotFound extends RuntimeException {
    }
}
