package com.nethub.engine.routing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DijkstraEngine {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GraphEdge {
        private String sourceRouter;
        private String targetRouter;
        private String outgoingInterface;
        private int cost;
        private String targetNetwork;
        private String subnetMask;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DijkstraStep {
        private int stepNumber;
        private String selectedNode;
        private Map<String, Integer> shortestDistances;
        private Map<String, String> predecessors;
        private String explanation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DijkstraResult {
        private String sourceRouter;
        private Map<String, Integer> shortestDistances;
        private Map<String, String> nextHops;
        private List<DijkstraStep> steps;
        private List<GraphEdge> graphEdges;
    }

    public DijkstraResult computeShortestPath(String sourceRouter, List<GraphEdge> edges) {
        // Collect all router vertices
        Set<String> vertices = new HashSet<>();
        vertices.add(sourceRouter);
        Map<String, List<GraphEdge>> adjacency = new HashMap<>();

        for (GraphEdge e : edges) {
            vertices.add(e.getSourceRouter());
            vertices.add(e.getTargetRouter());
            adjacency.computeIfAbsent(e.getSourceRouter(), k -> new ArrayList<>()).add(e);
        }

        Map<String, Integer> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        Map<String, String> firstHop = new HashMap<>();
        Set<String> unvisited = new HashSet<>(vertices);

        for (String v : vertices) {
            dist.put(v, Integer.MAX_VALUE / 2);
            prev.put(v, null);
            firstHop.put(v, null);
        }
        dist.put(sourceRouter, 0);

        List<DijkstraStep> steps = new ArrayList<>();
        int stepCount = 0;

        while (!unvisited.isEmpty()) {
            // Find unvisited vertex with smallest distance
            String current = null;
            int smallest = Integer.MAX_VALUE;
            for (String u : unvisited) {
                if (dist.get(u) < smallest) {
                    smallest = dist.get(u);
                    current = u;
                }
            }

            if (current == null || smallest >= Integer.MAX_VALUE / 2) {
                break; // Remaining nodes are unreachable
            }

            unvisited.remove(current);
            stepCount++;

            // Relax neighbors
            List<GraphEdge> neighbors = adjacency.getOrDefault(current, Collections.emptyList());
            for (GraphEdge edge : neighbors) {
                String neighbor = edge.getTargetRouter();
                if (unvisited.contains(neighbor)) {
                    int newDist = dist.get(current) + edge.getCost();
                    if (newDist < dist.get(neighbor)) {
                        dist.put(neighbor, newDist);
                        prev.put(neighbor, current);
                        // Track first hop outgoing interface from source
                        if (current.equals(sourceRouter)) {
                            firstHop.put(neighbor, edge.getOutgoingInterface());
                        } else {
                            firstHop.put(neighbor, firstHop.get(current));
                        }
                    }
                }
            }

            steps.add(DijkstraStep.builder()
                    .stepNumber(stepCount)
                    .selectedNode(current)
                    .shortestDistances(new HashMap<>(dist))
                    .predecessors(new HashMap<>(prev))
                    .explanation("Evaluated vertex " + current + " (cost=" + smallest + "). Relaxed adjacent edges to update tentative distance estimates.")
                    .build());
        }

        return DijkstraResult.builder()
                .sourceRouter(sourceRouter)
                .shortestDistances(dist)
                .nextHops(firstHop)
                .steps(steps)
                .graphEdges(edges)
                .build();
    }
}
