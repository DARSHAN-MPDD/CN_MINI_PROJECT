package com.network.simulator.controller;

import com.network.simulator.model.NetworkLink;
import com.network.simulator.model.NetworkNode;
import com.network.simulator.service.TopologyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TopologyController {
    private final TopologyService topologyService;

    public TopologyController(TopologyService topologyService) {
        this.topologyService = topologyService;
    }

    @GetMapping("/topology")
    public ResponseEntity<Map<String, Object>> getTopology() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("nodes", topologyService.getNodes().values());
        resp.put("links", topologyService.getLinks().values());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/topology/sample")
    public ResponseEntity<Map<String, Object>> loadSample() {
        topologyService.loadSampleTopology();
        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Sample topology loaded");
        resp.put("nodes", topologyService.getNodes().values());
        resp.put("links", topologyService.getLinks().values());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/nodes")
    public ResponseEntity<?> addNode(@RequestBody NetworkNode node) {
        try {
            NetworkNode added = topologyService.addNode(node);
            return ResponseEntity.status(HttpStatus.CREATED).body(added);
        } catch (IllegalArgumentException ex) {
            Map<String, String> err = new HashMap<>();
            err.put("error", ex.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PostMapping("/links")
    public ResponseEntity<?> addLink(@RequestBody NetworkLink link) {
        try {
            NetworkLink added = topologyService.addLink(link);
            return ResponseEntity.status(HttpStatus.CREATED).body(added);
        } catch (IllegalArgumentException ex) {
            Map<String, String> err = new HashMap<>();
            err.put("error", ex.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PostMapping("/links/{id}/fail")
    public ResponseEntity<?> failLink(@PathVariable("id") String linkId, @RequestBody(required = false) Map<String, String> body) {
        try {
            topologyService.failLink(linkId);
            Map<String, Object> resp = new HashMap<>();
            resp.put("message", "Link failed");
            resp.put("linkId", linkId);
            if (body != null && body.containsKey("source") && body.containsKey("destination")) {
                // Optionally could trigger reroute test - we'll let PacketController handle
                resp.put("rerouteTestRequested", true);
                resp.put("source", body.get("source"));
                resp.put("destination", body.get("destination"));
            }
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException ex) {
            Map<String, String> err = new HashMap<>();
            err.put("error", ex.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PostMapping("/links/{id}/recover")
    public ResponseEntity<?> recoverLink(@PathVariable("id") String linkId) {
        try {
            topologyService.recoverLink(linkId);
            Map<String, String> resp = new HashMap<>();
            resp.put("message", "Link recovered");
            resp.put("linkId", linkId);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException ex) {
            Map<String, String> err = new HashMap<>();
            err.put("error", ex.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }
}
