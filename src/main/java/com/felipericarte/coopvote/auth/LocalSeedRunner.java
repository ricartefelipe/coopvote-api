package com.felipericarte.coopvote.auth;

import java.time.Clock;
import java.util.EnumSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Associados sintéticos para a jornada local (gestora, auditora e eleitores). Só existe quando
 * {@code coopvote.auth.local-seed.enabled=true}; nunca habilitar em ambiente compartilhado.
 */
@Component
@ConditionalOnProperty(prefix = "coopvote.auth.local-seed", name = "enabled", havingValue = "true")
public class LocalSeedRunner implements ApplicationRunner {

    static final String PASSWORD = "coopvote-local";

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalSeedRunner.class);

    private final AssociateRepository associates;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public LocalSeedRunner(AssociateRepository associates, PasswordEncoder passwordEncoder, Clock clock) {
        this.associates = associates;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Associate> seed = List.of(
                Associate.register("gestora", "Gestora da assembleia", null, hash(),
                        EnumSet.of(AssociateRole.MANAGER, AssociateRole.VOTER), clock.instant()),
                Associate.register("auditora", "Auditora", null, hash(),
                        EnumSet.of(AssociateRole.AUDITOR), clock.instant()),
                Associate.register("associado-1", "Associado 1", null, hash(),
                        EnumSet.of(AssociateRole.VOTER), clock.instant()),
                Associate.register("associado-2", "Associado 2", null, hash(),
                        EnumSet.of(AssociateRole.VOTER), clock.instant()));
        int created = 0;
        for (Associate associate : seed) {
            if (associates.findByAssociateId(associate.getAssociateId()).isEmpty()) {
                associates.save(associate);
                created++;
            }
        }
        LOGGER.atWarn().addKeyValue("created", created)
                .log("Local synthetic associates seeded; this profile is not for shared environments");
    }

    private String hash() {
        return passwordEncoder.encode(PASSWORD);
    }
}
