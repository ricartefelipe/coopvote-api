package com.felipericarte.coopvote.voting;

import com.felipericarte.coopvote.agenda.Agenda;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Janela temporal única em que uma pauta recebe votos.
 */
@Entity
@Table(name = "voting_session")
public class VotingSession {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agenda_id", nullable = false, unique = true)
    private Agenda agenda;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private Instant openedAt;

    @Column(name = "closes_at", nullable = false, updatable = false)
    private Instant closesAt;

    protected VotingSession() {
        // Required by JPA.
    }

    private VotingSession(UUID id, Agenda agenda, Instant openedAt, Instant closesAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.agenda = Objects.requireNonNull(agenda, "agenda must not be null");
        this.openedAt = Objects.requireNonNull(openedAt, "openedAt must not be null");
        this.closesAt = Objects.requireNonNull(closesAt, "closesAt must not be null");
        if (!closesAt.isAfter(openedAt)) {
            throw new IllegalArgumentException("closesAt must be after openedAt");
        }
    }

    /**
     * Abre uma sessão a partir do instante informado e por uma duração positiva.
     */
    public static VotingSession open(Agenda agenda, Instant openedAt, Duration duration) {
        Objects.requireNonNull(duration, "duration must not be null");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("duration must be positive");
        }
        return new VotingSession(UUID.randomUUID(), agenda, openedAt, openedAt.plus(duration));
    }

    /**
     * Informa se um voto pode ser recebido no instante. O fechamento é um limite exclusivo.
     */
    public boolean isOpenAt(Instant instant) {
        Objects.requireNonNull(instant, "instant must not be null");
        return !instant.isBefore(openedAt) && instant.isBefore(closesAt);
    }

    public VotingSessionStatus statusAt(Instant instant) {
        return isOpenAt(instant) ? VotingSessionStatus.OPEN : VotingSessionStatus.CLOSED;
    }

    public UUID getId() {
        return id;
    }

    public Agenda getAgenda() {
        return agenda;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public Instant getClosesAt() {
        return closesAt;
    }
}

