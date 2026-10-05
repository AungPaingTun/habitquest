package com.habitquest.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("demo-seed")
class DemoSeedConfig {

    /** Replaces the system clock while seeding, so check-ins land on past days through the normal services. */
    @Bean
    @Primary
    MutableClock demoClock() {
        return new MutableClock();
    }
}
