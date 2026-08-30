package com.felipericarte.coopvote.eligibility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpUserInfoClientTest {

    private MockRestServiceServer server;
    private HttpUserInfoClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://eligibility.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HttpUserInfoClient(builder.build());
    }

    @Test
    void shouldMapAbleToVote() {
        server.expect(requestTo("http://eligibility.test/users/39053344705"))
                .andRespond(withSuccess("{\"status\":\"ABLE_TO_VOTE\"}", MediaType.APPLICATION_JSON));

        assertThat(client.findByCpf("39053344705")).contains(EligibilityStatus.ABLE_TO_VOTE);
        server.verify();
    }

    @Test
    void shouldMapNotFound() {
        server.expect(requestTo("http://eligibility.test/users/39053344705"))
                .andRespond(withResourceNotFound());

        assertThat(client.findByCpf("39053344705")).contains(EligibilityStatus.NOT_FOUND);
        server.verify();
    }
}
