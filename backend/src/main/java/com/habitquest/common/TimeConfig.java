package com.habitquest.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfig {

    /** One shared clock, so tests can replace it with a fixed date. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
