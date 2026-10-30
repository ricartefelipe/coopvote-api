package com.felipericarte.coopvote.auth.api;

import com.felipericarte.coopvote.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Login do associado e emissão do token de sessão")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica o associado e devolve o token Bearer")
    @ApiResponse(responseCode = "200", description = "Token emitido")
    @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(LoginResponse.from(authService.login(request.associateId(), request.password())));
    }
}
