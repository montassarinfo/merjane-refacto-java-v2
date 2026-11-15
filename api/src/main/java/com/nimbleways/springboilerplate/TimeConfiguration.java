package com.nimbleways.springboilerplate;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Supplies the system clock with the same default time zone as the legacy rules. */
@Configuration
public class TimeConfiguration {
    /**
     * Supplies the clock for date-sensitive services.
     * @return system clock in the default time zone
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
