package com.network.simulator.controller;

import com.network.simulator.model.FailureEvent;
import com.network.simulator.service.FailureHistoryService;
import com.network.simulator.service.NetworkFailureDetectionService;
import com.network.simulator.service.SelfHealingService;
import com.network.simulator.service.TopologyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FailureController {
    private final NetworkFailureDetectionService detectionService;
    private final SelfHealingService selfHealingService;
    private final FailureHistoryService historyService;
    private final TopologyService topologyService;

    public FailureController(NetworkFailureDetectionService detectionService,
                             SelfHealingService selfHealingService,
                             FailureHistoryService historyService,
                             TopologyService topologyService) {
        this.detectionService = detectionService;
        this.selfHealingService = selfHealingService;
        this.historyService = historyService;
        this.topologyService = topologyService;
    }

    public static class FailureRequest {
        private String id;
        private List<String> linkIds;
        private String source;
        private String destination;
        private List<String> oldRoute;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public List<String> getLinkIds() {
            return linkIds;
        }

        public void setLinkIds(List<String> linkIds) {
            this.linkIds = linkIds;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getDestination() {
            return destination;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }

        public List<String> getOldRoute() {
            return oldRoute;
        }

        public void setOldRoute(List<String> oldRoute) {
            this.oldRoute = oldRoute;
        }
    }

    @PostMapping("/failures/link/{id}")
    public ResponseEntity<?> failLink(@PathVariable("id") String linkId) {
        try {
            detectionService.detectLinkFailure(linkId);
            Map<String, String> resp = new HashMap<>();
            resp.put("message", "Link failure detected and applied");
            resp.put("linkId", linkId);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException ex) {
            Map<String, String> err = new HashMap<>();
            err.put("error", ex.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PostMapping("/failures/node/{id}")
    public ResponseEntity<?> failNode(@PathVariable("id") String nodeId) {
        try {
            detectionService.detectNodeFailure(nodeId);
            Map<String, String> resp = new HashMap<>();
            resp.put("message", "Node failure detected and applied");
            resp.put("nodeId", nodeId);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException ex) {
            Map<String, String> err = new HashMap<>();
            err.put("error", ex.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PostMapping("/failures/links")
    public ResponseEntity<?> failMultipleLinks(@RequestBody FailureRequest request) {
        try {
            detectionService.detectMultipleFailures(request == null ? null : request.getLinkIds());
            Map<String, Object> resp = new HashMap<>();
            resp.put("message", "Multiple link failures processed");
            resp.put("count", request == null || request.getLinkIds() == null ? 0 : request.getLinkIds().size());
            return ResponseEntity.ok(resp);
        } catch (Exception ex) {
            Map<String, String> err = new HashMap<>();
            err.put("error", ex.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PostMapping("/recovery/link/{id}")
    public ResponseEntity<?> recoverLink(@PathVariable("id") String linkId) {
        try {
            selfHealingService.recoverLink(linkId);
            Map<String, String> resp = new HashMap<>();
            resp.put("message", "Link recovery completed");
            resp.put("linkId", linkId);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException ex) {
            Map<String, String> err = new HashMap<>();
            err.put("error", ex.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PostMapping("/recovery/node/{id}")
    public ResponseEntity<?> recoverNode(@PathVariable("id") String nodeId) {
        try {
            selfHealingService.recoverNode(nodeId);
            Map<String, String> resp = new HashMap<>();
            resp.put("message", "Node recovery completed");
            resp.put("nodeId", nodeId);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException ex) {
            Map<String, String> err = new HashMap<>();
            err.put("error", ex.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @GetMapping("/failures/status")
    public ResponseEntity<Map<String, Object>> getFailureStatus() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("nodes", topologyService.getNodes().values());
        resp.put("links", topologyService.getLinks().values());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/failures/history")
    public ResponseEntity<List<FailureEvent>> getHistory() {
        return ResponseEntity.ok(historyService.getHistory());
    }

    @PostMapping("/routing/recalculate")
    public ResponseEntity<?> recalculateRoute(@RequestBody FailureRequest request) {
        if (request == null || request.getSource() == null || request.getDestination() == null) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "Source and destination are required");
            return ResponseEntity.badRequest().body(err);
        }
        SelfHealingService.HealingResult result = selfHealingService.attemptRecovery(
                request.getSource(), request.getDestination(), request.getOldRoute());
        return ResponseEntity.ok(result);
    }
}
