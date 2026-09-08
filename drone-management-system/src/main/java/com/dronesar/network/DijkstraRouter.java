package com.dronesar.network;

import java.util.*;

public class DijkstraRouter {

    public static List<String> findShortestPath(MeshNetworkGraph graph, String sourceId, String destinationId) {
        if (graph == null || sourceId == null || destinationId == null) {
            return Collections.emptyList();
        }

        NetworkNode source = graph.getNode(sourceId);
        NetworkNode destination = graph.getNode(destinationId);

        if (source == null || destination == null || !source.isOnline() || !destination.isOnline()) {
            return Collections.emptyList();
        }

        if (sourceId.equals(destinationId)) {
            return List.of(sourceId);
        }

        Map<String, Double> distances = new HashMap<>();
        Map<String, String> predecessors = new HashMap<>();
        PriorityQueue<NodeDistance> pq = new PriorityQueue<>(Comparator.comparingDouble(nd -> nd.distance));
        Set<String> visited = new HashSet<>();

        for (String nodeId : graph.getNodes().keySet()) {
            distances.put(nodeId, Double.POSITIVE_INFINITY);
        }

        distances.put(sourceId, 0.0);
        pq.offer(new NodeDistance(sourceId, 0.0));

        while (!pq.isEmpty()) {
            NodeDistance current = pq.poll();
            String uId = current.nodeId;

            if (visited.contains(uId)) continue;
            visited.add(uId);

            if (uId.equals(destinationId)) {
                break;
            }

            NetworkNode uNode = graph.getNode(uId);
            if (uNode == null || !uNode.isOnline()) continue;

            for (NetworkNode vNode : graph.getNeighbors(uId)) {
                String vId = vNode.getId();
                if (visited.contains(vId) || !vNode.isOnline()) continue;

                double edgeWeight = uNode.getPosition().distanceTo(vNode.getPosition());
                double newDist = distances.get(uId) + edgeWeight;

                if (newDist < distances.get(vId)) {
                    distances.put(vId, newDist);
                    predecessors.put(vId, uId);
                    pq.offer(new NodeDistance(vId, newDist));
                }
            }
        }

        if (!predecessors.containsKey(destinationId) && !sourceId.equals(destinationId)) {
            return Collections.emptyList();
        }

        LinkedList<String> path = new LinkedList<>();
        String step = destinationId;
        while (step != null) {
            path.addFirst(step);
            step = predecessors.get(step);
        }

        return path;
    }

    private static class NodeDistance {
        final String nodeId;
        final double distance;

        NodeDistance(String nodeId, double distance) {
            this.nodeId = nodeId;
            this.distance = distance;
        }
    }
}
