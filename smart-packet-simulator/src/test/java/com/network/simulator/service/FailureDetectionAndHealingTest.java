package com.network.simulator.service;

import com.network.simulator.model.LinkStatus;
import com.network.simulator.model.NetworkLink;
import com.network.simulator.model.NetworkNode;
import com.network.simulator.model.NodeStatus;
import com.network.simulator.model.NodeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class FailureDetectionAndHealingTest {
    private TopologyService topologyService;
    private RoutingService routingService;
    private PacketDeliveryService packetDeliveryService;
    private FailureHistoryService historyService;
    private NetworkFailureDetectionService detectionService;
    private SelfHealingService healingService;

    @BeforeEach
    void setUp() {
        topologyService = new TopologyService();
        routingService = new RoutingService();
        packetDeliveryService = new PacketDeliveryService(topologyService, routingService);
        historyService = new FailureHistoryService();
        detectionService = new NetworkFailureDetectionService(topologyService, historyService);
        healingService = new SelfHealingService(topologyService, routingService, historyService);
        historyService.clear();
        topologyService.loadSampleTopology();
    }

    @Test
    void detectLinkFailure() {
        detectionService.detectLinkFailure("L5");
        assertThat(topologyService.getTopology().getLink("L5").getStatus()).isEqualTo(LinkStatus.FAILED);
        assertThat(historyService.getHistory().size()).isGreaterThan(0);
    }

    @Test
    void detectNodeFailure() {
        detectionService.detectNodeFailure("R3");
        assertThat(topologyService.getTopology().getNode("R3").getStatus()).isEqualTo(NodeStatus.FAILED);
    }

    @Test
    void successfulAlternativeRouteCalculation() {
        detectionService.detectLinkFailure("L5");
        detectionService.detectLinkFailure("L6");
        SelfHealingService.HealingResult result = healingService.attemptRecovery("S1", "D1", Arrays.asList("S1", "R1", "R3", "D1"));
        assertThat(result.isRecovered()).isTrue();
        assertThat(result.getNewRoute()).isNotEmpty();
    }

    @Test
    void successfulRecovery() {
        detectionService.detectLinkFailure("L5");
        healingService.recoverLink("L5");
        assertThat(topologyService.getTopology().getLink("L5").getStatus()).isEqualTo(LinkStatus.ACTIVE);
    }

    @Test
    void recoveryWhenNoAlternativeRouteExists() {
        TopologyService ts = new TopologyService();
        ts.addNode(new NetworkNode("S", "S", NodeType.SOURCE, NodeStatus.ACTIVE));
        ts.addNode(new NetworkNode("D", "D", NodeType.DESTINATION, NodeStatus.ACTIVE));
        ts.addLink(new NetworkLink("L1", "S", "D", 1, LinkStatus.ACTIVE));
        ts.addLink(new NetworkLink("L2", "D", "S", 1, LinkStatus.ACTIVE));
        RoutingService rs = new RoutingService();
        FailureHistoryService hs = new FailureHistoryService();
        SelfHealingService shs = new SelfHealingService(ts, rs, hs);
        ts.failLink("L1");
        ts.failLink("L2");
        SelfHealingService.HealingResult res = shs.attemptRecovery("S", "D", Arrays.asList("S", "D"));
        assertThat(res.isRecovered()).isFalse();
    }

    @Test
    void multipleFailures() {
        detectionService.detectMultipleFailures(Arrays.asList("L1", "L3"));
        assertThat(topologyService.getTopology().getLink("L1").getStatus()).isEqualTo(LinkStatus.FAILED);
        assertThat(topologyService.getTopology().getLink("L3").getStatus()).isEqualTo(LinkStatus.FAILED);
    }

    @Test
    void nodeRecovery() {
        detectionService.detectNodeFailure("R1");
        healingService.recoverNode("R1");
        assertThat(topologyService.getTopology().getNode("R1").getStatus()).isEqualTo(NodeStatus.ACTIVE);
    }

    @Test
    void linkRecovery() {
        detectionService.detectLinkFailure("L1");
        healingService.recoverLink("L1");
        assertThat(topologyService.getTopology().getLink("L1").getStatus()).isEqualTo(LinkStatus.ACTIVE);
    }

    @Test
    void failureEventHistory() {
        historyService.clear();
        detectionService.detectLinkFailure("L1");
        assertThat(historyService.getHistory().size()).isGreaterThan(0);
    }

    @Test
    void recoveryEventHistory() {
        historyService.clear();
        detectionService.detectLinkFailure("L1");
        healingService.recoverLink("L1");
        assertThat(historyService.getHistory().size()).isGreaterThanOrEqualTo(2);
    }
}
