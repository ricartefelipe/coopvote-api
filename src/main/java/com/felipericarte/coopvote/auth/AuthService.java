package com.felipericarte.coopvote.auth;

import com.felipericarte.coopvote.shared.error.ApiException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Login por credencial local. Usuário inexistente, senha errada e conta inativa recebem a mesma
 * resposta, sem revelar qual condição falhou.
 */
@Service
public class AuthService {

    private final AssociateRepository associates;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;

    public AuthService(AssociateRepository associates, PasswordEncoder passwordEncoder, TokenService tokens) {
        this.associates = associates;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
    }

    @Transactional(readOnly = true)
    public TokenService.IssuedToken login(String associateId, String password) {
        Associate associate = associates.findByAssociateId(associateId.trim())
                .filter(Associate::isActive)
                .filter(found -> passwordEncoder.matches(password, found.getPasswordHash()))
                .orElseThrow(() -> ApiException.unauthorized(
                        "INVALID_CREDENTIALS",
                        "Credenciais inválidas."));
        return tokens.issue(associate);
    }
}
