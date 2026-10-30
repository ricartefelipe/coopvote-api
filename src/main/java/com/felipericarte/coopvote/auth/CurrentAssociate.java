package com.felipericarte.coopvote.auth;

import com.felipericarte.coopvote.shared.error.ApiException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Lê o associado autenticado do contexto de segurança. Sem token válido não há associado.
 */
@Component
public class CurrentAssociate {

    public AuthenticatedAssociate require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw ApiException.unauthorized("AUTHENTICATION_REQUIRED", "Autenticação obrigatória.");
        }
        Jwt jwt = jwtAuthentication.getToken();
        List<String> roles = jwt.getClaimAsStringList(TokenService.ROLES_CLAIM);
        Set<AssociateRole> parsed = roles == null
                ? EnumSet.noneOf(AssociateRole.class)
                : roles.stream()
                        .map(AssociateRole::valueOf)
                        .collect(Collectors.toCollection(() -> EnumSet.noneOf(AssociateRole.class)));
        return new AuthenticatedAssociate(jwt.getSubject(), parsed);
    }
}
