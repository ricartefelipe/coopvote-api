package com.felipericarte.coopvote.auth;

import com.felipericarte.coopvote.eligibility.VoterIdentity;
import com.felipericarte.coopvote.shared.error.ApiException;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Completa a identidade do token com o CPF do cadastro, única fonte usada pela elegibilidade.
 * Um associado desativado depois de receber o token deixa de votar na requisição seguinte.
 */
@Component
public class VoterIdentityResolver {

    private final AssociateRepository associates;

    public VoterIdentityResolver(AssociateRepository associates) {
        this.associates = associates;
    }

    @Transactional(readOnly = true)
    public VoterIdentity resolve(AuthenticatedAssociate authenticated) {
        Associate associate = associates.findByAssociateId(authenticated.associateId())
                .filter(Associate::isActive)
                .orElseThrow(() -> ApiException.forbidden(
                        "ASSOCIATE_INACTIVE",
                        "O associado não está ativo."));
        Optional<String> cpf = associate.getCpf();
        return new VoterIdentity(associate.getAssociateId(), cpf);
    }
}
