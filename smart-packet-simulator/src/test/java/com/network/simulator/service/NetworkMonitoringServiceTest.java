package com.network.simulator.service;

import com.network.simulator.model.LinkStatus;
import com.network.simulator.model.NetworkLink;
import com.network.simulator.model.NetworkNode;
import com.network.simulator.model.NodeStatus;
import com.network.simulator.model.NodeType;
import com.network.simulator.model.Packet;
import com.network.simulator.model.PacketStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NetworkMonitoringServiceTest {
    private TopologyService topologyService;
    private RoutingService routingService;
    private NetworkMonitoringService monitoring;
    private PacketDeliveryService delivery;

    @BeforeEach
    void setUp() {
        topologyService = new TopologyService();
        routingService = new RoutingService();
        monitoring = new NetworkMonitoringService(topologyService);
        delivery = new PacketDeliveryService(topologyService, routingService, monitoring);
        topologyService.loadSampleTopology();
    }

    @Test
    void zeroDenominatorsWhenNoPackets() {
        assertThat(monitoring.getTotalPacketsCreated()).isZero();
        assertThat(monitoring.getSuccessfullyDeliveredPackets()).isZero();
        assertThat(monitoring.getFailedPackets()).isZero();
        assertThat(monitoring.getPacketsInTransit()).isZero();
        assertThat(monitoring.getPacketDeliveryRatio()).isEqualTo(0.0);
        assertThat(monitoring.getPacketLossRatio()).isEqualTo(0.0);
        assertThat(monitoring.getPacketFailureRatio()).isEqualTo(0.0);
        assertThat(monitoring.getAveragePacketDeliveryTime()).isEqualTo(0.0);
        assertThat(monitoring.getMinDeliveryTime()).isNull();
        assertThat(monitoring.getMaxDeliveryTime()).isNull();
    }

    @Test
    void recordsDeliveredAndFailedPacketsFromRealEvents() {
        Packet ok = delivery.deliverPacket("S1", "D1", "hello");
        assertThat(ok.getStatus()).isEqualTo(PacketStatus.DELIVERED);
        assertThat(ok.getRoute()).isNotEmpty();
        assertThat(ok.getRouteCost()).isGreaterThan(0);

        Packet bad = delivery.deliverPacket("S1", "NOPE", "x");
        assertThat(bad.getStatus()).isEqualTo(PacketStatus.FAILED);
        assertThat(bad.getErrorMessage()).isNotBlank();

        assertThat(monitoring.getTotalPacketsCreated()).isEqualTo(2);
        assertThat(monitoring.getSuccessfullyDeliveredPackets()).isEqualTo(1);
        assertThat(monitoring.getFailedPackets()).isEqualTo(1);
        assertThat(monitoring.getPacketsInTransit()).isZero();
        assertThat(monitoring.getPacketDeliveryRatio()).isEqualTo(50.0);
        assertThat(monitoring.getPacketLossRatio()).isEqualTo(50.0);
        assertThat(monitoring.getPacketFailureRatio()).isEqualTo(50.0);
    }

    @Test
    void deliveryTimeStatistics() {
        delivery.deliverPacket("S1", "D1", "a");
        delivery.deliverPacket("S1", "D1", "b");
        assertThat(monitoring.getAveragePacketDeliveryTime()).isGreaterThanOrEqualTo(0.0);
        assertThat(monitoring.getMinDeliveryTime()).isNotNull();
        assertThat(monitoring.getMaxDeliveryTime()).isNotNull();
        assertThat(monitoring.getMinDeliveryTime()).isLessThanOrEqualTo(monitoring.getMaxDeliveryTime());
    }

    @Test
    void overviewContainsAllMetrics() {
        delivery.deliverPacket("S1", "D1", "a");
        var ov = monitoring.getOverview();
        assertThat(ov.get("totalPacketsCreated")).isEqualTo(1L);
        assertThat(ov.get("successfullyDeliveredPackets")).isEqualTo(1L);
        assertThat(ov.get("failedPackets")).isEqualTo(0L);
        assertThat(ov.get("packetsInTransit")).isEqualTo(0L);
        assertThat(ov.get("packetDeliveryRatio")).isEqualTo(100.0);
        assertThat(ov.get("packetFailureRatio")).isEqualTo(0.0);
        assertThat(ov.get("averagePacketDeliveryTime")).isInstanceOf(Number.class);
        assertThat(((Number) ov.get("averagePacketDeliveryTime")).doubleValue()).isGreaterThanOrEqualTo(0.0);
        assertThat(ov.get("totalActiveNodes")).isEqualTo(6);
        assertThat(ov.get("totalFailedNodes")).isEqualTo(0);
        assertThat(ov.get("totalActiveLinks")).isEqualTo(14);
        assertThat(ov.get("totalFailedLinks")).isEqualTo(0);
        assertThat(ov.get("totalNodes")).isEqualTo(6);
        assertThat(ov.get("totalLinks")).isEqualTo(14);
    }

    @Test
    void availabilityAllActive() {
        var avail = monitoring.getAvailability();
        assertThat(avail.get("totalNodes")).isEqualTo(6);
        assertThat(avail.get("failedNodes")).isEqualTo(0);
        assertThat(avail.get("nodeAvailabilityPercent")).isEqualTo(100.0);
        assertThat(avail.get("totalLinks")).isEqualTo(14);
        assertThat(avail.get("failedLinks")).isEqualTo(0);
        assertThat(avail.get("linkAvailabilityPercent")).isEqualTo(100.0);
        assertThat(avail.get("overallAvailabilityPercent")).isEqualTo(100.0);
        assertThat(avail.get("formula")).isNotNull();
        assertThat(avail.get("formula").toString()).contains("totalActiveNodes");
    }

    @Test
    void availabilityReflectsFailures() {
        topologyService.failLink("L5");
        topologyService.getTopology().getNode("R1").setStatus(NodeStatus.FAILED);
        var avail = monitoring.getAvailability();
        assertThat(avail.get("failedNodes")).isEqualTo(1);
        assertThat(avail.get("failedLinks")).isEqualTo(1);
        double overall = ((Number) avail.get("overallAvailabilityPercent")).doubleValue();
        assertThat(overall).isLessThan(100.0);
        assertThat(overall).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void availabilityEmptyTopologyZeroDenominator() {
        TopologyService empty = new TopologyService();
        NetworkMonitoringService emptyMonitoring = new NetworkMonitoringService(empty);
        var avail = emptyMonitoring.getAvailability();
        assertThat(avail.get("totalNodes")).isEqualTo(0);
        assertThat(avail.get("totalLinks")).isEqualTo(0);
        assertThat(avail.get("nodeAvailabilityPercent")).isEqualTo(100.0);
        assertThat(avail.get("linkAvailabilityPercent")).isEqualTo(100.0);
        assertThat(avail.get("overallAvailabilityPercent")).isEqualTo(100.0);
    }

    @Test
    void linkUtilizationIsSimulatedAndZeroInitially() {
        var links = monitoring.getLinkUtilization();
        assertThat(links).hasSize(14);
        for (var l : links) {
            assertThat(l.getConfiguredCapacity()).isEqualTo(10);
            assertThat(l.getUtilizationPercentage()).isEqualTo(0.0);
            assertThat(l.isCongested()).isFalse();
            assertThat(l.getStatusNote()).contains("Simulated");
            assertThat(l.getStatusNote()).contains("not actual physical bandwidth");
        }
    }

    @Test
    void linkUtilizationAfterDelivery() {
        delivery.deliverPacket("S1", "D1", "a");
        var links = monitoring.getLinkUtilization();
        var l1 = links.stream().filter(link -> "L1".equals(link.getLinkId())).findFirst().orElseThrow();
        assertThat(l1.getPacketCount()).isGreaterThanOrEqualTo(1);
        assertThat(l1.getUtilizationPercentage()).isGreaterThan(0.0);
    }

    @Test
    void historySnapshot() {
        assertThat(monitoring.getHistory()).isEmpty();
        monitoring.takeSnapshot();
        assertThat(monitoring.getHistory()).hasSize(1);
        var snap = monitoring.getHistory().get(0);
        assertThat(snap.getTimestamp()).isNotNull();
        assertThat(snap.getTotalPackets()).isEqualTo(monitoring.getTotalPacketsCreated());
        assertThat(snap.getActiveNodes()).isEqualTo(6);
        assertThat(snap.getActiveLinks()).isEqualTo(14);
    }
}
