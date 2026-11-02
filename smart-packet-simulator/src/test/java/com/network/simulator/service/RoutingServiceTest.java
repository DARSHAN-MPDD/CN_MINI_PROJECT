package com.network.simulator.service;

import com.network.simulator.model.LinkStatus;
import com.network.simulator.model.NetworkLink;
import com.network.simulator.model.NetworkNode;
import com.network.simulator.model.NetworkTopology;
import com.network.simulator.model.NodeStatus;
import com.network.simulator.model.NodeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoutingServiceTest {
    private TopologyService topologyService;
    private RoutingService routingService;

    @BeforeEach
    void setUp() {
        topologyService = new TopologyService();
        routingService = new RoutingService();
        topologyService.loadSampleTopology();
    }

    @Test
    void findShortestValidRoute() {
        RoutingService.RouteResult result = routingService.findShortestPath(topologyService.getTopology(), "S1", "D1");
        assertThat(result.isFound()).isTrue();
        assertThat(result.getPath()).isNotEmpty();
        assertThat(result.getCost()).isEqualTo(7.0);
        assertThat(result.getPath()).containsExactly("S1", "R1", "R3", "D1");
    }

    @Test
    void ignoreFailedLink() {
        topologyService.failLink("L5");
        topologyService.failLink("L6");
        RoutingService.RouteResult result = routingService.findShortestPath(topologyService.getTopology(), "S1", "D1");
        assertThat(result.isFound()).isTrue();
        assertThat(result.getCost()).isEqualTo(8.0);
    }

    @Test
    void ignoreFailedNode() {
        topologyService.getTopology().getNode("R3").setStatus(NodeStatus.FAILED);
        RoutingService.RouteResult result = routingService.findShortestPath(topologyService.getTopology(), "S1", "D1");
        assertThat(result.isFound()).isTrue();
        assertThat(result.getCost()).isEqualTo(8.0);
    }

    @Test
    void unreachableDestination() {
        TopologyService ts = new TopologyService();
        ts.addNode(new NetworkNode("A", "A", NodeType.SOURCE, NodeStatus.ACTIVE));
        ts.addNode(new NetworkNode("B", "B", NodeType.DESTINATION, NodeStatus.ACTIVE));
        RoutingService rs = new RoutingService();
        RoutingService.RouteResult result = rs.findShortestPath(ts.getTopology(), "A", "B");
        assertThat(result.isFound()).isFalse();
    }

    @Test
    void invalidSourceOrDestination() {
        RoutingService.RouteResult result1 = routingService.findShortestPath(topologyService.getTopology(), "NOPE", "D1");
        assertThat(result1.isFound()).isFalse();
        RoutingService.RouteResult result2 = routingService.findShortestPath(topologyService.getTopology(), "S1", "NOPE");
        assertThat(result2.isFound()).isFalse();
    }

    @Test
    void rejectInvalidLinkCosts() {
        TopologyService ts = new TopologyService();
        ts.addNode(new NetworkNode("A", "A", NodeType.SOURCE, NodeStatus.ACTIVE));
        ts.addNode(new NetworkNode("B", "B", NodeType.DESTINATION, NodeStatus.ACTIVE));
        try {
            ts.addLink(new NetworkLink("L_BAD", "A", "B", 0, LinkStatus.ACTIVE));
            assertThat(false).isTrue();
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage()).contains("positive");
        }
        try {
            ts.addLink(new NetworkLink("L_BAD2", "A", "B", -1, LinkStatus.ACTIVE));
            assertThat(false).isTrue();
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage()).contains("positive");
        }
    }
}
