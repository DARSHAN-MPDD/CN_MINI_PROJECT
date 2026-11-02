package com.network.simulator.service;

import com.network.simulator.model.Packet;
import com.network.simulator.model.PacketStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PacketDeliveryServiceTest {
    private TopologyService topologyService;
    private RoutingService routingService;
    private PacketDeliveryService packetDeliveryService;

    @BeforeEach
    void setUp() {
        topologyService = new TopologyService();
        routingService = new RoutingService();
        packetDeliveryService = new PacketDeliveryService(topologyService, routingService);
        topologyService.loadSampleTopology();
    }

    @Test
    void successfulPacketDelivery() {
        Packet packet = packetDeliveryService.deliverPacket("S1", "D1", "Hello");
        assertThat(packet.getStatus()).isEqualTo(PacketStatus.DELIVERED);
        assertThat(packet.getRoute()).isNotEmpty();
        assertThat(packet.getRouteCost()).isGreaterThan(0);
    }

    @Test
    void failedPacketDeliveryWhenNoRouteExists() {
        // Isolate - remove all links from S1
        topologyService.getLinks().values().stream()
                .filter(l -> l.getSourceNodeId().equals("S1") || l.getTargetNodeId().equals("S1"))
                .forEach(l -> l.setStatus(com.network.simulator.model.LinkStatus.FAILED));
        Packet packet = packetDeliveryService.deliverPacket("S1", "D1", "Hello");
        assertThat(packet.getStatus()).isEqualTo(PacketStatus.FAILED);
        assertThat(packet.getErrorMessage()).isNotBlank();
    }

    @Test
    void reroutingAfterLinkFailure() {
        Packet p1 = packetDeliveryService.deliverPacket("S1", "D1", "test");
        assertThat(p1.getStatus()).isEqualTo(PacketStatus.DELIVERED);
        // Fail the link that would break primary route? Primary goes S1-R1 (L1/L2), R1-R3 (L5/L6). Fail L5,L6
        topologyService.failLink("L5");
        topologyService.failLink("L6");
        Packet p2 = packetDeliveryService.deliverPacket("S1", "D1", "test2");
        assertThat(p2.getStatus()).isEqualTo(PacketStatus.DELIVERED);
        assertThat(p2.getRoute()).isNotEqualTo(p1.getRoute());
    }

    @Test
    void noAlternativeRouteAfterFailure() {
        // Create a simple topology where only one route exists
        TopologyService ts = new TopologyService();
        ts.addNode(new com.network.simulator.model.NetworkNode("S", "S", com.network.simulator.model.NodeType.SOURCE, com.network.simulator.model.NodeStatus.ACTIVE));
        ts.addNode(new com.network.simulator.model.NetworkNode("D", "D", com.network.simulator.model.NodeType.DESTINATION, com.network.simulator.model.NodeStatus.ACTIVE));
        ts.addLink(new com.network.simulator.model.NetworkLink("L1", "S", "D", 1, com.network.simulator.model.LinkStatus.ACTIVE));
        ts.addLink(new com.network.simulator.model.NetworkLink("L2", "D", "S", 1, com.network.simulator.model.LinkStatus.ACTIVE));
        RoutingService rs = new RoutingService();
        PacketDeliveryService pds = new PacketDeliveryService(ts, rs);
        ts.failLink("L1");
        ts.failLink("L2");
        Packet p = pds.deliverPacket("S", "D", "x");
        assertThat(p.getStatus()).isEqualTo(PacketStatus.FAILED);
    }

    @Test
    void loadSampleTopology() {
        TopologyService ts = new TopologyService();
        ts.loadSampleTopology();
        assertThat(ts.getNodes().size()).isGreaterThanOrEqualTo(6);
    }
}
