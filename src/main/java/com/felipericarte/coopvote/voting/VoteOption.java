package com.felipericarte.coopvote.voting;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

/**
 * Opções válidas de voto expostas no contrato da API.
 */
public enum VoteOption {
    YES("Sim"),
    NO("Não");

    private final String value;

    VoteOption(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    /**
     * Converte o valor JSON no enum, aceitando diferenças de caixa e espaços externos.
     */
    @JsonCreator
    public static VoteOption fromValue(String value) {
        if (value == null) {
            return null;
        }

        return Arrays.stream(values())
                .filter(option -> option.value.equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("vote must be 'Sim' or 'Não'"));
    }
}

