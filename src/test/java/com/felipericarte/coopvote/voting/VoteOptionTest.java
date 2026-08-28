package com.felipericarte.coopvote.voting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class VoteOptionTest {

    @Test
    void shouldReadDocumentedOptionsIgnoringCase() {
        assertThat(VoteOption.fromValue(" sim ")).isEqualTo(VoteOption.YES);
        assertThat(VoteOption.fromValue("NÃO")).isEqualTo(VoteOption.NO);
    }

    @Test
    void shouldRejectUnknownOption() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> VoteOption.fromValue("Talvez"))
                .withMessage("vote must be 'Sim' or 'Não'");
    }
}

