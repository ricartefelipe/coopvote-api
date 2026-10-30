package com.felipericarte.coopvote.support;

import com.felipericarte.coopvote.auth.Associate;
import com.felipericarte.coopvote.auth.AssociateRepository;
import com.felipericarte.coopvote.auth.AssociateRole;
import com.felipericarte.coopvote.auth.TokenService;
import java.time.Clock;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Cria associados sintéticos e devolve o header Bearer correspondente.
 */
@TestComponent
public class TestAssociates {

    public static final String PASSWORD = "test-password";

    private final AssociateRepository associates;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;
    private final Clock clock;

    public TestAssociates(AssociateRepository associates, PasswordEncoder passwordEncoder, TokenService tokens, Clock clock) {
        this.associates = associates;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.clock = clock;
    }

    public Associate create(String associateId, Set<AssociateRole> roles) {
        return create(associateId, null, roles);
    }

    public Associate create(String associateId, String cpf, Set<AssociateRole> roles) {
        return associates.save(Associate.register(
                associateId, "Associado " + associateId, cpf, passwordEncoder.encode(PASSWORD), roles, clock.instant()));
    }

    public String bearer(Associate associate) {
        return "Bearer " + tokens.issue(associate).token();
    }

    public String voter(String associateId) {
        return bearer(create(associateId, EnumSet.of(AssociateRole.VOTER)));
    }

    public String manager(String associateId) {
        return bearer(create(associateId, EnumSet.of(AssociateRole.MANAGER, AssociateRole.VOTER)));
    }

    public String auditor(String associateId) {
        return bearer(create(associateId, EnumSet.of(AssociateRole.AUDITOR)));
    }

    public void clear() {
        associates.deleteAll();
    }
}
