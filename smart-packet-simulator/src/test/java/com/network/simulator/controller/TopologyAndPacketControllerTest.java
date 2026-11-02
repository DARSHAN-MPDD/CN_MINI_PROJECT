package com.network.simulator.controller;

import com.network.simulator.model.PacketStatus;
import com.network.simulator.service.TopologyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TopologyAndPacketControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTpl;

    @Autowired
    private TopologyService topologyService;

    @BeforeEach
    void reset() {
        topologyService.clear();
    }

    @Test
    void healthStillWorks() {
        ResponseEntity<com.fasterxml.jackson.databind.JsonNode> response =
                restTpl.getForEntity("http://localhost:" + port + "/api/health", com.fasterxml.jackson.databind.JsonNode.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().get("status").asText()).isEqualTo("UP");
    }

    @Test
    void loadSampleAndDeliverPacket() {
        restTpl.postForEntity("http://localhost:" + port + "/api/topology/sample", null, Map.class);
        Map<String, Object> req = new HashMap<>();
        req.put("source", "S1");
        req.put("destination", "D1");
        req.put("payload", "test");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(req, headers);
        ResponseEntity<Map> resp = restTpl.postForEntity("http://localhost:" + port + "/api/packets/deliver", entity, Map.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody().get("status")).isEqualTo("DELIVERED");
    }
}
