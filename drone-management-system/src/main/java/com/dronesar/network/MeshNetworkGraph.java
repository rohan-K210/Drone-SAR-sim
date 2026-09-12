package com.dronesar.network;

import java.util.*;

public class MeshNetworkGraph {
    private final Map<String, NetworkNode> nodes = new HashMap<>();
    private final Map<String, List<NetworkNode>> adjacencyList = new HashMap<>();

    public void addNode(NetworkNode node) {
        nodes.put(node.getId(), node);
        adjacencyList.putIfAbsent(node.getId(), new ArrayList<>());
    }

    public void clear() {
        nodes.clear();
        adjacencyList.clear();
    }

    public void updateEdges() {
        for (String id : nodes.keySet()) {
            adjacencyList.put(id, new ArrayList<>());
        }

        List<NetworkNode> nodeList = new ArrayList<>(nodes.values());
        for (int i = 0; i < nodeList.size(); i++) {
            NetworkNode u = nodeList.get(i);
            if (!u.isOnline()) continue;

            for (int j = 0; j < nodeList.size(); j++) {
                if (i == j) continue;
                NetworkNode v = nodeList.get(j);
                if (!v.isOnline()) continue;

                if (u.canReach(v)) {
                    adjacencyList.get(u.getId()).add(v);
                }
            }
        }
    }

    public Map<String, NetworkNode> getNodes() {
        return Collections.unmodifiableMap(nodes);
    }

    public NetworkNode getNode(String id) {
        return nodes.get(id);
    }

    public List<NetworkNode> getNeighbors(String nodeId) {
        return adjacencyList.getOrDefault(nodeId, Collections.emptyList());
    }

    public List<LinkSegment> getAllActiveLinks() {
        List<LinkSegment> links = new ArrayList<>();
        Set<String> seenPairs = new HashSet<>();

        for (Map.Entry<String, List<NetworkNode>> entry : adjacencyList.entrySet()) {
            String uId = entry.getKey();
            NetworkNode u = nodes.get(uId);
            if (u == null || !u.isOnline()) continue;

            for (NetworkNode v : entry.getValue()) {
                if (!v.isOnline()) continue;
                String pairKey = uId.compareTo(v.getId()) < 0 ? (uId + "<->" + v.getId()) : (v.getId() + "<->" + uId);
                if (!seenPairs.contains(pairKey)) {
                    seenPairs.add(pairKey);
                    links.add(new LinkSegment(u, v));
                }
            }
        }
        return links;
    }

    public static class LinkSegment {
        private final NetworkNode nodeA;
        private final NetworkNode nodeB;

        public LinkSegment(NetworkNode a, NetworkNode b) {
            this.nodeA = a;
            this.nodeB = b;
        }

        public NetworkNode getNodeA() {
            return nodeA;
        }

        public NetworkNode getNodeB() {
            return nodeB;
        }
    }
}
