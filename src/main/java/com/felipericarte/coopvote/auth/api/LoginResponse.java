package com.felipericarte.coopvote.auth.api;

import com.felipericarte.coopvote.auth.TokenService;
import java.time.Instant;
import java.util.List;

public record LoginResponse(String tokenType, String accessToken, Instant expiresAt, List<String> roles) {

    public static LoginResponse from(TokenService.IssuedToken issued) {
        return new LoginResponse("Bearer", issued.token(), issued.expiresAt(), issued.roles());
    }
}
