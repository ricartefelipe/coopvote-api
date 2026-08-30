package com.felipericarte.coopvote.eligibility;

import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(EligibilityProperties.class)
public class EligibilityConfig {

    @Bean
    RestClient eligibilityRestClient(EligibilityProperties properties) {
        var requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .connectTimeout(properties.timeout())
                        .build());
        requestFactory.setReadTimeout(properties.timeout());
        return RestClient.builder()
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory)
                .build();
    }
}
