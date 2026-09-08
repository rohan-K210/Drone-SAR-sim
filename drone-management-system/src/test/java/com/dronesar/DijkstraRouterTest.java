package com.dronesar;

import com.dronesar.model.Position;
import com.dronesar.network.DijkstraRouter;
import com.dronesar.network.MeshNetworkGraph;
import com.dronesar.network.NetworkNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DijkstraRouterTest {

    private MeshNetworkGraph graph;

    @BeforeEach
    public void setUp() {
        graph = new MeshNetworkGraph();
    }

    @Test
    public void testDirectSingleHopRoute() {
        NetworkNode tower = new NetworkNode("TOWER", new Position(0, 0), 100, true, true);
        NetworkNode drone = new NetworkNode("DRONE-1", new Position(50, 0), 100, false, true);

        graph.addNode(tower);
        graph.addNode(drone);
        graph.updateEdges();

        List<String> path = DijkstraRouter.findShortestPath(graph, "DRONE-1", "TOWER");
        assertEquals(List.of("DRONE-1", "TOWER"), path);
    }

    @Test
    public void testMultiHopRelayThroughIntermediateDrone() {
        NetworkNode tower = new NetworkNode("TOWER", new Position(0, 0), 100, true, true);
        NetworkNode d1 = new NetworkNode("DRONE-1", new Position(80, 0), 100, false, true);
        NetworkNode d2 = new NetworkNode("DRONE-2", new Position(160, 0), 100, false, true);
        NetworkNode d3 = new NetworkNode("DRONE-3", new Position(240, 0), 100, false, true);

        graph.addNode(tower);
        graph.addNode(d1);
        graph.addNode(d2);
        graph.addNode(d3);
        graph.updateEdges();

        List<String> path = DijkstraRouter.findShortestPath(graph, "DRONE-3", "TOWER");
        assertEquals(List.of("DRONE-3", "DRONE-2", "DRONE-1", "TOWER"), path);
    }

    @Test
    public void testDynamicRerouteOnNodeFailure() {
        NetworkNode start = new NetworkNode("START", new Position(0, 50), 70, false, true);
        NetworkNode top = new NetworkNode("TOP", new Position(50, 10), 70, false, true);
        NetworkNode bottom = new NetworkNode("BOTTOM", new Position(50, 90), 70, false, true);
        NetworkNode tower = new NetworkNode("TOWER", new Position(100, 50), 70, true, true);

        graph.addNode(start);
        graph.addNode(top);
        graph.addNode(bottom);
        graph.addNode(tower);
        graph.updateEdges();

        List<String> initialPath = DijkstraRouter.findShortestPath(graph, "START", "TOWER");
        assertTrue(initialPath.contains("START") && initialPath.contains("TOWER"));

        graph.clear();
        graph.addNode(start);
        graph.addNode(new NetworkNode("TOP", new Position(50, 10), 70, false, false));
        graph.addNode(bottom);
        graph.addNode(tower);
        graph.updateEdges();

        List<String> reroutedPath = DijkstraRouter.findShortestPath(graph, "START", "TOWER");
        assertEquals(List.of("START", "BOTTOM", "TOWER"), reroutedPath);
    }
}
