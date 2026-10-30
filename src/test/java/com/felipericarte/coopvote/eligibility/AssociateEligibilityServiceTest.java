package com.felipericarte.coopvote.eligibility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.felipericarte.coopvote.shared.error.ApiException;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssociateEligibilityServiceTest {

    private static final String VALID_CPF = "39053344705";

    @Mock
    private UserInfoClient userInfoClient;

    @Test
    void shouldSkipLookupForAssociatesWithoutCpf() {
        AssociateEligibilityService service = service(true);
        assertThatCode(() -> service.assertEligible(new VoterIdentity("associate-123", Optional.empty())))
                .doesNotThrowAnyException();
        verifyNoInteractions(userInfoClient);
    }

    @Test
    void shouldSkipLookupWhenDisabled() {
        AssociateEligibilityService service = service(false);
        assertThatCode(() -> service.assertEligible(withCpf(VALID_CPF))).doesNotThrowAnyException();
        verifyNoInteractions(userInfoClient);
    }

    @Test
    void shouldRejectInvalidCheckDigitsWithoutCallingTheExternalService() {
        AssociateEligibilityService service = service(true);
        assertThatThrownBy(() -> service.assertEligible(withCpf("11111111111")))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("INVALID_CPF");
                    assertThat(exception.getStatus().value()).isEqualTo(403);
                });
        verifyNoInteractions(userInfoClient);
    }

    @Test
    void shouldRejectUnableAssociates() {
        when(userInfoClient.findByCpf(VALID_CPF)).thenReturn(Optional.of(EligibilityStatus.UNABLE_TO_VOTE));
        AssociateEligibilityService service = service(true);
        assertThatThrownBy(() -> service.assertEligible(withCpf(VALID_CPF)))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("ASSOCIATE_UNABLE_TO_VOTE"));
    }

    @Test
    void shouldRejectUnknownCpfsFromTheExternalService() {
        when(userInfoClient.findByCpf(VALID_CPF)).thenReturn(Optional.of(EligibilityStatus.NOT_FOUND));
        AssociateEligibilityService service = service(true);
        assertThatThrownBy(() -> service.assertEligible(withCpf(VALID_CPF)))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("ASSOCIATE_NOT_FOUND"));
    }

    @Test
    void shouldAllowEligibleAssociates() {
        when(userInfoClient.findByCpf(VALID_CPF)).thenReturn(Optional.of(EligibilityStatus.ABLE_TO_VOTE));
        AssociateEligibilityService service = service(true);
        assertThatCode(() -> service.assertEligible(withCpf(VALID_CPF))).doesNotThrowAnyException();
    }

    @Test
    void shouldFailClosedWhenTheExternalServiceIsUnavailable() {
        when(userInfoClient.findByCpf(VALID_CPF)).thenReturn(Optional.empty());
        AssociateEligibilityService service = service(true);
        assertThatThrownBy(() -> service.assertEligible(withCpf(VALID_CPF)))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("ELIGIBILITY_UNAVAILABLE");
                    assertThat(exception.getStatus().value()).isEqualTo(503);
                });
    }

    private static VoterIdentity withCpf(String cpf) {
        return new VoterIdentity("associate-cpf", Optional.of(cpf));
    }

    private AssociateEligibilityService service(boolean enabled) {
        return new AssociateEligibilityService(
                new EligibilityProperties(enabled, URI.create("http://eligibility.test"), Duration.ofSeconds(1)),
                userInfoClient);
    }
}
