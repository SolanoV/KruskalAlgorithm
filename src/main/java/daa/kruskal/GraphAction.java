package daa.kruskal;

import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphAction {
    String type;
    int source;
    int destination;
    int weight;
    int vertices;
    List<WeightedGraph.EdgeInfo> edges;
    List<Point2D> positions;
    int[] labelMapping;
    Map<Integer, Integer> reverseMapping;

    GraphAction(String type, int source, int destination, int weight) {
        this.type = type;
        this.source = source;
        this.destination = destination;
        this.weight = weight;
    }

    GraphAction(String type, int vertices, List<WeightedGraph.EdgeInfo> edges, List<Point2D> positions,
                int[] labelMapping, Map<Integer, Integer> reverseMapping) {
        this.type = type;
        this.vertices = vertices;
        this.edges = edges != null ? new ArrayList<>(edges) : new ArrayList<>();
        this.positions = positions != null ? new ArrayList<>(positions) : new ArrayList<>();
        this.labelMapping = labelMapping != null ? labelMapping.clone() : null;
        this.reverseMapping = reverseMapping != null ? new HashMap<>(reverseMapping) : new HashMap<>();
    }
}