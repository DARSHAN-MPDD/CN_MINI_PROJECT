package com.network.simulator.controller;

import com.network.simulator.model.HealthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes operational endpoints for the simulator.
 */
@RestController
public class HealthController {

    /**
     * Health-check endpoint used by the frontend and tooling to verify the
     * backend is running and responsive.
     *
     * @return HTTP 200 with a JSON body containing the application status and name.
     */
    @GetMapping("/api/health")
    public ResponseEntity<HealthResponse> health() {
        HealthResponse response = new HealthResponse("UP", "Smart Packet Simulator");
        return ResponseEntity.ok(response);
    }
}