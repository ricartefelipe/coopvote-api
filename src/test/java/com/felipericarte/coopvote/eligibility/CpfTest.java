package com.felipericarte.coopvote.eligibility;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CpfTest {

    @Test
    void shouldParseFormattedAndRejectNonCpfIdentifiers() {
        assertThat(Cpf.parse("390.533.447-05")).contains("39053344705");
        assertThat(Cpf.parse("associate-123")).isEmpty();
        assertThat(Cpf.parse("123")).isEmpty();
    }

    @Test
    void shouldValidateOfficialCheckDigits() {
        assertThat(Cpf.hasValidCheckDigits("39053344705")).isTrue();
        assertThat(Cpf.hasValidCheckDigits("11111111111")).isFalse();
        assertThat(Cpf.hasValidCheckDigits("39053344700")).isFalse();
    }
}
