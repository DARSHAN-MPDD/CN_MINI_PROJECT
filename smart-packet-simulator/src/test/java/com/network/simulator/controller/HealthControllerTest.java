
package com.network.simulator.controller;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTpl;

    @Test
    void healthEndpointReturnsExpectedStatus() {
        ResponseEntity<JsonNode> response =
                restTpl.getForEntity(
                        "http://localhost:" + port + "/api/health",
                        JsonNode.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status").asText()).isEqualTo("UP");
        assertThat(response.getBody().get("application").asText())
                .isEqualTo("Smart Packet Simulator");
    }
}
