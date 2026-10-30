package com.felipericarte.coopvote.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Associado com credencial local. O CPF é opcional e só existe para consultar a elegibilidade;
 * nunca é usado como credencial.
 */
@Entity
@Table(name = "associate")
public class Associate {

    @Id
    private UUID id;

    @Column(name = "associate_id", nullable = false, updatable = false, length = 100)
    private String associateId;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "cpf", length = 11)
    private String cpf;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "roles", nullable = false, length = 100)
    private String roles;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Associate() {
        // Required by JPA.
    }

    private Associate(
            String associateId,
            String displayName,
            String cpf,
            String passwordHash,
            Set<AssociateRole> roles,
            Instant createdAt) {
        this.id = UUID.randomUUID();
        this.associateId = requireText(associateId, "associateId");
        this.displayName = requireText(displayName, "displayName");
        this.cpf = cpf;
        this.passwordHash = requireText(passwordHash, "passwordHash");
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("roles must not be empty");
        }
        this.roles = roles.stream().map(AssociateRole::name).sorted().collect(Collectors.joining(","));
        this.active = true;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static Associate register(
            String associateId,
            String displayName,
            String cpf,
            String passwordHash,
            Set<AssociateRole> roles,
            Instant createdAt) {
        return new Associate(associateId, displayName, cpf, passwordHash, roles, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public String getAssociateId() {
        return associateId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Optional<String> getCpf() {
        return Optional.ofNullable(cpf);
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Set<AssociateRole> getRoles() {
        return Arrays.stream(roles.split(","))
                .map(AssociateRole::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(AssociateRole.class)));
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        this.active = false;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
