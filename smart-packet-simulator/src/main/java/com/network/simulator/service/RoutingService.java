package com.network.simulator.service;

import com.network.simulator.model.LinkStatus;
import com.network.simulator.model.NetworkLink;
import com.network.simulator.model.NetworkNode;
import com.network.simulator.model.NetworkTopology;
import com.network.simulator.model.NodeStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Implements Dijkstra's shortest-path algorithm for routing.
 */
@Service
public class RoutingService {

    public static class RouteResult {
        private final List<String> path;
        private final double cost;
        private final boolean found;

        public RouteResult(List<String> path, double cost, boolean found) {
            this.path = path == null ? Collections.emptyList() : new ArrayList<>(path);
            this.cost = cost;
            this.found = found;
        }

        public List<String> getPath() {
            return path;
        }

        public double getCost() {
            return cost;
        }

        public boolean isFound() {
            return found;
        }
    }

    public RouteResult findShortestPath(NetworkTopology topology, String sourceId, String destinationId) {
        if (topology == null) {
            return new RouteResult(Collections.emptyList(), 0.0, false);
        }
        if (sourceId == null || destinationId == null) {
            return new RouteResult(Collections.emptyList(), 0.0, false);
        }
        if (sourceId.equals(destinationId)) {
            // Source equal to destination - return direct path with cost 0 as reasonable behavior
            return new RouteResult(Collections.singletonList(sourceId), 0.0, true);
        }
        NetworkNode sourceNode = topology.getNode(sourceId);
        NetworkNode destNode = topology.getNode(destinationId);
        if (sourceNode == null || destNode == null) {
            return new RouteResult(Collections.emptyList(), 0.0, false);
        }
        if (sourceNode.getStatus() == NodeStatus.FAILED || destNode.getStatus() == NodeStatus.FAILED) {
            return new RouteResult(Collections.emptyList(), 0.0, false);
        }

        // Build adjacency: node -> list of (neighbor, link)
        Map<String, List<Map.Entry<String, NetworkLink>>> graph = new HashMap<>();
        for (NetworkLink link : topology.getLinks().values()) {
            if (link.getStatus() == LinkStatus.FAILED) {
                continue;
            }
            NetworkNode s = topology.getNode(link.getSourceNodeId());
            NetworkNode t = topology.getNode(link.getTargetNodeId());
            if (s == null || t == null || s.getStatus() == NodeStatus.FAILED || t.getStatus() == NodeStatus.FAILED) {
                continue;
            }
            graph.computeIfAbsent(link.getSourceNodeId(), k -> new ArrayList<>())
                    .add(new java.util.AbstractMap.SimpleEntry<>(link.getTargetNodeId(), link));
        }

        // Dijkstra
        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        PriorityQueue<Map.Entry<String, Double>> pq = new PriorityQueue<>(Comparator.comparingDouble(Map.Entry::getValue));
        dist.put(sourceId, 0.0);
        pq.offer(new java.util.AbstractMap.SimpleEntry<>(sourceId, 0.0));

        while (!pq.isEmpty()) {
            Map.Entry<String, Double> cur = pq.poll();
            String curNode = cur.getKey();
            double curDist = cur.getValue();
            if (curDist > dist.getOrDefault(curNode, Double.POSITIVE_INFINITY)) {
                continue;
            }
            if (curNode.equals(destinationId)) {
                break;
            }
            List<Map.Entry<String, NetworkLink>> neighbors = graph.getOrDefault(curNode, Collections.emptyList());
            for (Map.Entry<String, NetworkLink> neighborEntry : neighbors) {
                String nextNode = neighborEntry.getKey();
                NetworkNode nextNodeObj = topology.getNode(nextNode);
                if (nextNodeObj == null || nextNodeObj.getStatus() == NodeStatus.FAILED) {
                    continue;
                }
                NetworkLink link = neighborEntry.getValue();
                if (link.getStatus() == LinkStatus.FAILED) {
                    continue;
                }
                double newDist = curDist + link.getCost();
                if (newDist < dist.getOrDefault(nextNode, Double.POSITIVE_INFINITY)) {
                    dist.put(nextNode, newDist);
                    prev.put(nextNode, curNode);
                    pq.offer(new java.util.AbstractMap.SimpleEntry<>(nextNode, newDist));
                }
            }
        }

        if (!dist.containsKey(destinationId) || dist.get(destinationId) == Double.POSITIVE_INFINITY) {
            return new RouteResult(Collections.emptyList(), 0.0, false);
        }

        // Reconstruct path
        List<String> path = new ArrayList<>();
        String at = destinationId;
        List<String> rev = new ArrayList<>();
        while (at != null) {
            rev.add(at);
            if (at.equals(sourceId)) {
                break;
            }
            at = prev.get(at);
        }
        if (rev.isEmpty() || !rev.get(rev.size() - 1).equals(sourceId)) {
            return new RouteResult(Collections.emptyList(), 0.0, false);
        }
        Collections.reverse(rev);
        path.addAll(rev);
        return new RouteResult(path, dist.get(destinationId), true);
    }
}
