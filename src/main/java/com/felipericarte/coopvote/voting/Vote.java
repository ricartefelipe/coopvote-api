package com.felipericarte.coopvote.voting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Escolha imutável de um associado em uma sessão de votação.
 */
@Entity
@Table(name = "vote")
public class Vote {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voting_session_id", nullable = false, updatable = false)
    private VotingSession votingSession;

    @Column(name = "associate_id", nullable = false, updatable = false, length = 100)
    private String associateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "vote_option", nullable = false, updatable = false, length = 3)
    private VoteOption option;

    @Column(name = "cast_at", nullable = false, updatable = false)
    private Instant castAt;

    protected Vote() {
        // Required by JPA.
    }

    private Vote(
            UUID id,
            VotingSession votingSession,
            String associateId,
            VoteOption option,
            Instant castAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.votingSession = Objects.requireNonNull(votingSession, "votingSession must not be null");
        this.associateId = requireText(associateId);
        this.option = Objects.requireNonNull(option, "option must not be null");
        this.castAt = Objects.requireNonNull(castAt, "castAt must not be null");
    }

    /**
     * Registra uma escolha já validada pelo caso de uso de votação.
     */
    public static Vote cast(
            VotingSession votingSession,
            String associateId,
            VoteOption option,
            Instant castAt) {
        return new Vote(UUID.randomUUID(), votingSession, associateId, option, castAt);
    }

    public UUID getId() {
        return id;
    }

    public VotingSession getVotingSession() {
        return votingSession;
    }

    public String getAssociateId() {
        return associateId;
    }

    public VoteOption getOption() {
        return option;
    }

    public Instant getCastAt() {
        return castAt;
    }

    private static String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("associateId must not be blank");
        }
        return value.trim();
    }
}

