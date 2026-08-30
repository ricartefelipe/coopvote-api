package com.felipericarte.coopvote.eligibility;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "coopvote.eligibility")
public record EligibilityProperties(
        boolean enabled,
        URI baseUrl,
        Duration timeout) {

    public EligibilityProperties {
        if (baseUrl == null) {
            baseUrl = URI.create("https://user-info.herokuapp.com");
        }
        if (timeout == null) {
            timeout = Duration.ofSeconds(2);
        }
    }
}
