package com.felipericarte.coopvote;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Ponto de entrada da CoopVote API.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class CoopVoteApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoopVoteApplication.class, args);
    }
}
