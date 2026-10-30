package com.felipericarte.coopvote.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssociateRepository extends JpaRepository<Associate, UUID> {

    Optional<Associate> findByAssociateId(String associateId);
}
