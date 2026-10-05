package com.habitquest.demo;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A clock the demo seeder can move forward day by day. Only used by the demo-seed profile. */
class MutableClock extends Clock {

    private volatile Instant instant = Instant.now();

    void set(Instant instant) {
        this.instant = instant;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        // UserClock asks for the user's zone; keep reading the same moving instant.
        MutableClock parent = this;
        return new Clock() {
            @Override public ZoneId getZone() { return zone; }
            @Override public Clock withZone(ZoneId other) { return parent.withZone(other); }
            @Override public Instant instant() { return parent.instant(); }
        };
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
