    package daa.kruskal;

    public class test {
        public static void main(String[] args) {
            WeightedGraph graph = new WeightedGraph(5);

            // Add edges (src, dest, weight)
            graph.addEdge(0, 1, 4); // Edge from 0 to 1 with weight 4
            graph.addEdge(0, 2, 8); // Edge from 0 to 2 with weight 8
            graph.addEdge(1, 2, 2); // Edge from 1 to 2 with weight 2
            graph.addEdge(1, 3, 5); // Edge from 1 to 3 with weight 5
            graph.addEdge(2, 3, 5); // Edge from 2 to 3 with weight 5
            graph.addEdge(2, 4, 9); // Edge from 2 to 4 with weight 9
            graph.addEdge(3, 4, 4); // Edge from 3 to 4 with weight 4

            // Print the graph
            graph.printGraph();
        }
    }
