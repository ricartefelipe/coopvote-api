package com.felipericarte.coopvote.eligibility;

import java.util.Arrays;
import java.util.Optional;

public enum EligibilityStatus {
    ABLE_TO_VOTE,
    UNABLE_TO_VOTE,
    NOT_FOUND;

    static Optional<EligibilityStatus> fromExternal(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(status -> status != NOT_FOUND && status.name().equalsIgnoreCase(value.trim()))
                .findFirst();
    }
}
