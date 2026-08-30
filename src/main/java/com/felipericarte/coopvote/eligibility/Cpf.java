package com.felipericarte.coopvote.eligibility;

import java.util.Optional;

public final class Cpf {

    private Cpf() {
    }

    public static Optional<String> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String digits = raw.replaceAll("\\D", "");
        return digits.length() == 11 ? Optional.of(digits) : Optional.empty();
    }

    public static boolean hasValidCheckDigits(String digits) {
        if (digits == null || digits.length() != 11 || digits.chars().distinct().count() == 1) {
            return false;
        }
        return checkDigit(digits, 9) == digitAt(digits, 9)
                && checkDigit(digits, 10) == digitAt(digits, 10);
    }

    private static int checkDigit(String digits, int length) {
        int sum = 0;
        for (int index = 0; index < length; index++) {
            sum += digitAt(digits, index) * (length + 1 - index);
        }
        int remainder = (sum * 10) % 11;
        return remainder == 10 ? 0 : remainder;
    }

    private static int digitAt(String digits, int index) {
        return digits.charAt(index) - '0';
    }
}
