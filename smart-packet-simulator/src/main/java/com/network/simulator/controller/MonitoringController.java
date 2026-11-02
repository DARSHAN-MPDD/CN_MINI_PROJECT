package com.network.simulator.controller;

import com.network.simulator.service.NetworkMonitoringService;
import com.network.simulator.service.TopologyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {
    private final NetworkMonitoringService monitoringService;
    private final TopologyService topologyService;

    public MonitoringController(NetworkMonitoringService monitoringService, TopologyService topologyService) {
        this.monitoringService = monitoringService;
        this.topologyService = topologyService;
    }

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        return monitoringService.getOverview();
    }

    @GetMapping("/packets")
    public List<?> packets() {
        return monitoringService.getPacketRecords();
    }

    @GetMapping("/performance")
    public Map<String, Object> performance() {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("packetDeliveryRatio", monitoringService.getPacketDeliveryRatio());
        m.put("packetLossRatio", monitoringService.getPacketLossRatio());
        m.put("packetFailureRatio", monitoringService.getPacketFailureRatio());
        m.put("averagePacketDeliveryTime", monitoringService.getAveragePacketDeliveryTime());
        m.put("minDeliveryTime", monitoringService.getMinDeliveryTime());
        m.put("maxDeliveryTime", monitoringService.getMaxDeliveryTime());
        return m;
    }

    @GetMapping("/links")
    public List<?> links() {
        return monitoringService.getLinkUtilization();
    }

    @GetMapping("/history")
    public List<?> history() {
        return monitoringService.getHistory();
    }

    @GetMapping("/availability")
    public Map<String, Object> availability() {
        return monitoringService.getAvailability();
    }

    @PostMapping("/snapshot")
    public Object snapshot() {
        return monitoringService.takeSnapshot();
    }
}
