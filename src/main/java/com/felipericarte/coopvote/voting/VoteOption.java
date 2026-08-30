package com.felipericarte.coopvote.voting;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum VoteOption {
    YES("Sim"),
    NO("Não");

    private static final Map<String, VoteOption> BY_VALUE = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(
                    option -> option.value.toLowerCase(Locale.ROOT),
                    Function.identity()));

    private final String value;

    VoteOption(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static VoteOption fromValue(String value) {
        if (value == null) {
            return null;
        }
        VoteOption option = BY_VALUE.get(value.trim().toLowerCase(Locale.ROOT));
        if (option == null) {
            throw new IllegalArgumentException("vote must be 'Sim' or 'Não'");
        }
        return option;
    }
}
