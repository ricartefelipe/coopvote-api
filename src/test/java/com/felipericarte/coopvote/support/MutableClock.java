package com.felipericarte.coopvote.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Relógio controlável e seguro para testes concorrentes.
 */
public final class MutableClock extends Clock {

    private final AtomicReference<Instant> currentInstant;

    public MutableClock(Instant initialInstant) {
        this.currentInstant = new AtomicReference<>(Objects.requireNonNull(initialInstant));
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        if (!ZoneOffset.UTC.equals(zone)) {
            throw new IllegalArgumentException("MutableClock only supports UTC");
        }
        return this;
    }

    @Override
    public Instant instant() {
        return currentInstant.get();
    }

    public void set(Instant instant) {
        currentInstant.set(Objects.requireNonNull(instant));
    }

    public void advance(Duration duration) {
        currentInstant.updateAndGet(instant -> instant.plus(duration));
    }
}

