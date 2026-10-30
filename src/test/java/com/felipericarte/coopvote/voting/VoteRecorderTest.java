package com.felipericarte.coopvote.voting;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class VoteRecorderTest {

    @Test
    void shouldRecognizeOnlyTheUniqueVoteConstraintAsDuplicate() {
        assertThat(VoteRecorder.isDuplicateVote(violation("23505",
                "ERROR: duplicate key value violates unique constraint \"uk_vote_session_associate\""))).isTrue();
    }

    @Test
    void shouldNotTranslateOtherUniqueConstraintsAsDuplicateVote() {
        assertThat(VoteRecorder.isDuplicateVote(violation("23505",
                "ERROR: duplicate key value violates unique constraint \"uk_voting_session_agenda\""))).isFalse();
    }

    @Test
    void shouldNotTranslateForeignKeyOrOtherFailuresAsDuplicateVote() {
        assertThat(VoteRecorder.isDuplicateVote(violation("23503",
                "ERROR: insert or update on table \"vote\" violates foreign key constraint"))).isFalse();
        assertThat(VoteRecorder.isDuplicateVote(new DataIntegrityViolationException("sem causa SQL"))).isFalse();
    }

    private static DataIntegrityViolationException violation(String sqlState, String message) {
        return new DataIntegrityViolationException("falha", new SQLException(message, sqlState));
    }
}
