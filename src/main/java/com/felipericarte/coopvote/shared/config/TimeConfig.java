package com.felipericarte.coopvote.shared.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configura a fonte de tempo da aplicação.
 *
 * <p>O relógio é injetável para que regras temporais possam ser testadas sem esperas reais.</p>
 */
@Configuration
public class TimeConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}

