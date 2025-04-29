package daa.kruskal;

import java.util.*;

public class WeightedGraph {
    private int vertices;
    private List<List<Edge>> adjacent;

    public WeightedGraph(int vertices) {
        this.vertices = vertices;
        this.adjacent = new ArrayList<>(vertices);
        for (int i = 0; i < vertices; i++) {
            adjacent.add(new ArrayList<>());
        }
    }

    public void addEdge(int source, int destination, int weight) {
        adjacent.get(source).add(new Edge(destination, weight));
        adjacent.get(destination).add(new Edge(source, weight));
    }

    public List<Edge> getNeighbors(int vertex) {
        return adjacent.get(vertex);
    }

    public int getVertices() {
        return vertices;
    }

    public void printGraph() {
        for (int i = 0; i < vertices; i++) {
            System.out.print("Vertex " + i + ": ");
            for (Edge edge : adjacent.get(i)) {
                System.out.print(edge + " ");
            }
            System.out.println();
        }
    }

    // New method to get all unique edges for Kruskal's algorithm
    public List<EdgeInfo> getAllEdges() {
        List<EdgeInfo> edges = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int u = 0; u < vertices; u++) {
            for (Edge edge : adjacent.get(u)) {
                int v = edge.getDestination();
                // Use a unique key to avoid duplicates (u-v and v-u are the same)
                String key = Math.min(u, v) + "-" + Math.max(u, v);
                if (!seen.contains(key)) {
                    seen.add(key);
                    edges.add(new EdgeInfo(u, v, edge.getWeight()));
                }
            }
        }
        return edges;
    }

    // Helper class to store edge information (source, destination, weight)
    public static class EdgeInfo {
        int source;
        int destination;
        int weight;

        EdgeInfo(int source, int destination, int weight) {
            this.source = source;
            this.destination = destination;
            this.weight = weight;
        }
    }
}