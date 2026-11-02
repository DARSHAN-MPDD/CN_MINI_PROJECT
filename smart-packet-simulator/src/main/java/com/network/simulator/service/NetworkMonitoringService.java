package com.network.simulator.service;

import com.network.simulator.dto.LinkUtilizationDto;
import com.network.simulator.model.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class NetworkMonitoringService {
    private final TopologyService topologyService;
    private final List<PacketDeliveryRecord> packetRecords = Collections.synchronizedList(new ArrayList<PacketDeliveryRecord>());
    private final List<MonitoringSnapshot> history = Collections.synchronizedList(new ArrayList<MonitoringSnapshot>());
    private final Map<String, AtomicLong> linkPacketCounts = new ConcurrentHashMap<String, AtomicLong>();

    public NetworkMonitoringService(TopologyService topologyService) {
        this.topologyService = topologyService;
    }

    public void recordPacket(Packet packet) {
        if (packet == null) {
            return;
        }
        PacketDeliveryRecord record = new PacketDeliveryRecord();
        record.setPacketId(packet.getPacketId());
        record.setSourceNodeId(packet.getSourceNodeId());
        record.setDestinationNodeId(packet.getDestinationNodeId());
        record.setRoute(packet.getRoute() == null ? new ArrayList<String>() : new ArrayList<String>(packet.getRoute()));
        record.setStatus(packet.getStatus());
        record.setFailureReason(packet.getErrorMessage());
        record.setCreatedAt(Instant.now());
        if (packet.getStatus() == PacketStatus.DELIVERED) {
            record.setDeliveredAt(Instant.now());
            Long duration = null;
            if (record.getCreatedAt() != null && record.getDeliveredAt() != null) {
                duration = record.getDeliveredAt().toEpochMilli() - record.getCreatedAt().toEpochMilli();
            }
            record.setDeliveryDurationMillis(duration);
            if (packet.getRoute() != null && packet.getRoute().size() >= 2) {
                for (int i = 0; i < packet.getRoute().size() - 1; i++) {
                    String a = packet.getRoute().get(i);
                    String b = packet.getRoute().get(i + 1);
                    linkPacketCounts.computeIfAbsent(a + "-" + b, k -> new AtomicLong()).incrementAndGet();
                    NetworkTopology topology = topologyService.getTopology();
                    if (topology != null && topology.getLinks() != null) {
                        for (Map.Entry<String, NetworkLink> e : topology.getLinks().entrySet()) {
                            NetworkLink link = e.getValue();
                            if (link != null && ((link.getSourceNodeId().equals(a) && link.getTargetNodeId().equals(b))
                                    || (link.getSourceNodeId().equals(b) && link.getTargetNodeId().equals(a)))) {
                                linkPacketCounts.computeIfAbsent(link.getId(), k -> new AtomicLong()).incrementAndGet();
                            }
                        }
                    }
                }
            }
        }
        packetRecords.add(record);
    }

    public long getTotalPacketsCreated() {
        return packetRecords.size();
    }

    public long getSuccessfullyDeliveredPackets() {
        long count = 0;
        for (PacketDeliveryRecord r : packetRecords) {
            if (r.getStatus() == PacketStatus.DELIVERED) {
                count++;
            }
        }
        return count;
    }

    public long getFailedPackets() {
        long count = 0;
        for (PacketDeliveryRecord r : packetRecords) {
            if (r.getStatus() == PacketStatus.FAILED) {
                count++;
            }
        }
        return count;
    }

    public long getPacketsInTransit() {
        long count = 0;
        for (PacketDeliveryRecord r : packetRecords) {
            if (r.getStatus() == PacketStatus.IN_TRANSIT) {
                count++;
            }
        }
        return count;
    }

    public double getPacketDeliveryRatio() {
        long total = getTotalPacketsCreated();
        if (total == 0) {
            return 0.0;
        }
        return (getSuccessfullyDeliveredPackets() * 100.0) / (double) total;
    }

    public double getPacketLossRatio() {
        long total = getTotalPacketsCreated();
        if (total == 0) {
            return 0.0;
        }
        return (getFailedPackets() * 100.0) / (double) total;
    }

    public double getPacketFailureRatio() {
        return getPacketLossRatio();
    }

    public double getAveragePacketDeliveryTime() {
        List<Long> durations = new ArrayList<Long>();
        for (PacketDeliveryRecord r : packetRecords) {
            if (r.getStatus() == PacketStatus.DELIVERED && r.getDeliveryDurationMillis() != null) {
                durations.add(r.getDeliveryDurationMillis());
            }
        }
        if (durations.isEmpty()) {
            return 0.0;
        }
        long sum = 0;
        for (Long d : durations) {
            sum += d;
        }
        return (double) sum / (double) durations.size();
    }

    public Long getMinDeliveryTime() {
        Long min = null;
        for (PacketDeliveryRecord r : packetRecords) {
            if (r.getStatus() == PacketStatus.DELIVERED && r.getDeliveryDurationMillis() != null) {
                if (min == null || r.getDeliveryDurationMillis() < min) {
                    min = r.getDeliveryDurationMillis();
                }
            }
        }
        return min;
    }

    public Long getMaxDeliveryTime() {
        Long max = null;
        for (PacketDeliveryRecord r : packetRecords) {
            if (r.getStatus() == PacketStatus.DELIVERED && r.getDeliveryDurationMillis() != null) {
                if (max == null || r.getDeliveryDurationMillis() > max) {
                    max = r.getDeliveryDurationMillis();
                }
            }
        }
        return max;
    }

    public List<PacketDeliveryRecord> getPacketRecords() {
        return new ArrayList<PacketDeliveryRecord>(packetRecords);
    }

    public Map<String, Object> getOverview() {
        long totalPackets = getTotalPacketsCreated();
        long delivered = getSuccessfullyDeliveredPackets();
        long failed = getFailedPackets();
        long inTransit = getPacketsInTransit();
        double pdr = totalPackets == 0 ? 0.0 : (delivered * 100.0 / (double) totalPackets);
        double plr = totalPackets == 0 ? 0.0 : (failed * 100.0 / (double) totalPackets);
        double avgDelivery = getAveragePacketDeliveryTime();
        Long minDelivery = getMinDeliveryTime();
        Long maxDelivery = getMaxDeliveryTime();
        NetworkTopology topology = topologyService.getTopology();
        int totalNodes = topology == null || topology.getNodes() == null ? 0 : topology.getNodes().size();
        int failedNodes = 0;
        if (topology != null && topology.getNodes() != null) {
            for (NetworkNode node : topology.getNodes().values()) {
                if (node != null && node.getStatus() == NodeStatus.FAILED) {
                    failedNodes++;
                }
            }
        }
        int activeNodes = Math.max(0, totalNodes - failedNodes);
        int totalLinks = topology == null || topology.getLinks() == null ? 0 : topology.getLinks().size();
        int failedLinks = 0;
        if (topology != null && topology.getLinks() != null) {
            for (NetworkLink link : topology.getLinks().values()) {
                if (link != null && link.getStatus() == LinkStatus.FAILED) {
                    failedLinks++;
                }
            }
        }
        int activeLinks = Math.max(0, totalLinks - failedLinks);
        Map<String, Object> overview = new HashMap<String, Object>();
        overview.put("totalPacketsCreated", totalPackets);
        overview.put("successfullyDeliveredPackets", delivered);
        overview.put("failedPackets", failed);
        overview.put("packetsInTransit", inTransit);
        overview.put("packetDeliveryRatio", pdr);
        overview.put("packetFailureRatio", plr);
        overview.put("averagePacketDeliveryTime", avgDelivery);
        overview.put("minDeliveryTime", minDelivery == null ? 0L : minDelivery);
        overview.put("maxDeliveryTime", maxDelivery == null ? 0L : maxDelivery);
        overview.put("totalActiveNodes", activeNodes);
        overview.put("totalFailedNodes", failedNodes);
        overview.put("totalActiveLinks", activeLinks);
        overview.put("totalFailedLinks", failedLinks);
        overview.put("totalNodes", totalNodes);
        overview.put("totalLinks", totalLinks);
        return overview;
    }

    public Map<String, Object> getAvailability() {
        NetworkTopology topology = topologyService.getTopology();
        int totalNodes = topology == null || topology.getNodes() == null ? 0 : topology.getNodes().size();
        int failedNodes = 0;
        if (topology != null && topology.getNodes() != null) {
            for (NetworkNode node : topology.getNodes().values()) {
                if (node != null && node.getStatus() == NodeStatus.FAILED) {
                    failedNodes++;
                }
            }
        }
        int totalLinks = topology == null || topology.getLinks() == null ? 0 : topology.getLinks().size();
        int failedLinks = 0;
        if (topology != null && topology.getLinks() != null) {
            for (NetworkLink link : topology.getLinks().values()) {
                if (link != null && link.getStatus() == LinkStatus.FAILED) {
                    failedLinks++;
                }
            }
        }
        double nodeAvailability = totalNodes == 0 ? 100.0 : ((totalNodes - failedNodes) * 100.0 / (double) totalNodes);
        double linkAvailability = totalLinks == 0 ? 100.0 : ((totalLinks - failedLinks) * 100.0 / (double) totalLinks);
        double overallAvailability = (nodeAvailability + linkAvailability) / 2.0;
        Map<String, Object> res = new HashMap<String, Object>();
        res.put("totalNodes", totalNodes);
        res.put("failedNodes", failedNodes);
        res.put("nodeAvailabilityPercent", nodeAvailability);
        res.put("totalLinks", totalLinks);
        res.put("failedLinks", failedLinks);
        res.put("linkAvailabilityPercent", linkAvailability);
        res.put("overallAvailabilityPercent", overallAvailability);
        res.put("formula", "Node availability=((totalActiveNodes)/(totalNodes))*100; Link availability=((totalActiveLinks)/(totalLinks))*100; Overall=(nodeAvailability+linkAvailability)/2");
        return res;
    }

    public List<LinkUtilizationDto> getLinkUtilization() {
        List<LinkUtilizationDto> list = new ArrayList<LinkUtilizationDto>();
        NetworkTopology topology = topologyService.getTopology();
        if (topology == null || topology.getLinks() == null) {
            return list;
        }
        for (Map.Entry<String, NetworkLink> e : topology.getLinks().entrySet()) {
            NetworkLink link = e.getValue();
            if (link == null) {
                continue;
            }
            long count = linkPacketCounts.getOrDefault(link.getId(), new AtomicLong(0)).get();
            if (count == 0) {
                count = Math.max(count, linkPacketCounts.getOrDefault(link.getSourceNodeId() + "-" + link.getTargetNodeId(), new AtomicLong(0)).get());
                count = Math.max(count, linkPacketCounts.getOrDefault(link.getTargetNodeId() + "-" + link.getSourceNodeId(), new AtomicLong(0)).get());
            }
            int capacity = 10;
            double utilization = capacity <= 0 ? 0.0 : Math.min(100.0, (count * 100.0) / (double) capacity);
            LinkUtilizationDto dto = new LinkUtilizationDto();
            dto.setLinkId(link.getId());
            dto.setSourceNodeId(link.getSourceNodeId());
            dto.setTargetNodeId(link.getTargetNodeId());
            dto.setPacketCount((int) count);
            dto.setConfiguredCapacity(capacity);
            dto.setUtilizationPercentage(utilization);
            boolean congested = utilization >= 70.0 || link.getStatus() == LinkStatus.FAILED;
            dto.setCongested(congested);
            dto.setStatusNote("Simulated utilization (not actual physical bandwidth). Link status: " + (link.getStatus() == null ? "ACTIVE" : link.getStatus()));
            list.add(dto);
        }
        return list;
    }

    public MonitoringSnapshot takeSnapshot() {
        MonitoringSnapshot snapshot = new MonitoringSnapshot();
        snapshot.setTotalPackets(getTotalPacketsCreated());
        snapshot.setDeliveredPackets(getSuccessfullyDeliveredPackets());
        snapshot.setFailedPackets(getFailedPackets());
        snapshot.setPacketDeliveryRatio(getPacketDeliveryRatio());
        snapshot.setAverageDeliveryTime(getAveragePacketDeliveryTime());
        NetworkTopology topology = topologyService.getTopology();
        int totalNodes = topology == null || topology.getNodes() == null ? 0 : topology.getNodes().size();
        int failedNodes = 0;
        if (topology != null && topology.getNodes() != null) {
            for (NetworkNode node : topology.getNodes().values()) {
                if (node != null && node.getStatus() == NodeStatus.FAILED) {
                    failedNodes++;
                }
            }
        }
        int totalLinks = topology == null || topology.getLinks() == null ? 0 : topology.getLinks().size();
        int failedLinks = 0;
        if (topology != null && topology.getLinks() != null) {
            for (NetworkLink link : topology.getLinks().values()) {
                if (link != null && link.getStatus() == LinkStatus.FAILED) {
                    failedLinks++;
                }
            }
        }
        snapshot.setActiveNodes(Math.max(0, totalNodes - failedNodes));
        snapshot.setFailedNodes(failedNodes);
        snapshot.setActiveLinks(Math.max(0, totalLinks - failedLinks));
        snapshot.setFailedLinks(failedLinks);
        history.add(snapshot);
        return snapshot;
    }

    public List<MonitoringSnapshot> getHistory() {
        return new ArrayList<MonitoringSnapshot>(history);
    }
}
