package com.habitquest.common;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/** Answers "what date is it for this user?" using their own time zone, not the server's. */
@Component
public class UserClock {

    private final Clock clock;

    public UserClock(Clock clock) {
        this.clock = clock;
    }

    public LocalDate today(String timezone) {
        return LocalDate.now(clock.withZone(ZoneId.of(timezone)));
    }
}
