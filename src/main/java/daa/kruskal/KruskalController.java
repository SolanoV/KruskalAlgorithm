package daa.kruskal;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;

import java.util.*;

public class KruskalController {
    @FXML
    private TextField verticesField;
    @FXML
    private TextField srcField;
    @FXML
    private TextField destField;
    @FXML
    private TextField weightField;
    @FXML
    private Button createGraphButton;
    @FXML
    private Button addEdgeButton;
    @FXML
    private Button mstButton;
    @FXML
    private Button clearButton;
    @FXML
    private Button prevStepButton;
    @FXML
    private Button nextStepButton;
    @FXML
    private Label errorLabel;
    @FXML
    private Label stepIndicatorLabel;
    @FXML
    private Pane graphPane;
    @FXML
    private TableView<MSTStep> mstTableView;
    @FXML
    private TableColumn<MSTStep, String> edgeColumn;
    @FXML
    private TableColumn<MSTStep, Integer> weightColumn;
    @FXML
    private TableColumn<MSTStep, Boolean> statusColumn;
    @FXML
    private TableColumn<MSTStep, Integer> totalWeightColumn;

    private WeightedGraph graph;
    private Circle[] vertexCircles;
    private Text[] vertexLabels;
    private double vertexRadius = 15; // Radius of vertex circles
    private int[] vertexLabelMapping; // Maps internal index to display label
    private Map<Integer, Integer> reverseLabelMapping; // Maps display label to internal index
    private List<Point2D> vertexPositions; // Stores current vertex positions
    private Random random = new Random();
    private Map<String, Line> edgeLines; // Stores edge lines for dynamic updates
    private Map<String, Text> edgeWeightLabels; // Stores edge weight labels
    private double dragStartX, dragStartY; // For dragging vertices
    private List<WeightedGraph.EdgeInfo> allEdges; // All edges for MST
    private List<WeightedGraph.EdgeInfo> mstEdges; // MST edges
    private int currentStep; // Current step in MST process
    private int totalWeight; // Total MST weight

    // Class to represent an MST step in the TableView
    public static class MSTStep {
        private final SimpleStringProperty edge;
        private final SimpleIntegerProperty weight;
        private final SimpleBooleanProperty accepted;
        private final SimpleIntegerProperty totalWeight;

        public MSTStep(String edge, int weight, boolean accepted, int totalWeight) {
            this.edge = new SimpleStringProperty(edge);
            this.weight = new SimpleIntegerProperty(weight);
            this.accepted = new SimpleBooleanProperty(accepted);
            this.totalWeight = new SimpleIntegerProperty(totalWeight);
        }

        public String getEdge() {
            return edge.get();
        }

        public SimpleStringProperty edgeProperty() {
            return edge;
        }

        public int getWeight() {
            return weight.get();
        }

        public SimpleIntegerProperty weightProperty() {
            return weight;
        }

        public boolean isAccepted() {
            return accepted.get();
        }

        public SimpleBooleanProperty acceptedProperty() {
            return accepted;
        }

        public int getTotalWeight() {
            return totalWeight.get();
        }

        public SimpleIntegerProperty totalWeightProperty() {
            return totalWeight;
        }
    }

    @FXML
    private void initialize() {
        srcField.setDisable(true);
        destField.setDisable(true);
        weightField.setDisable(true);
        addEdgeButton.setDisable(true);
        mstButton.setDisable(true);
        prevStepButton.setDisable(true);
        nextStepButton.setDisable(true);
        stepIndicatorLabel.setText("");

        // Initialize TableView columns
        edgeColumn.setCellValueFactory(cellData -> cellData.getValue().edgeProperty());
        weightColumn.setCellValueFactory(cellData -> cellData.getValue().weightProperty().asObject());
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().acceptedProperty());
        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean accepted, boolean empty) {
                super.updateItem(accepted, empty);
                if (empty || accepted == null) {
                    setText(null);
                } else {
                    setText(accepted ? "Accepted" : "Rejected");
                }
            }
        });
        totalWeightColumn.setCellValueFactory(cellData -> cellData.getValue().totalWeightProperty().asObject());
    }

    @FXML
    private void createGraph() {
        try {
            int vertices = Integer.parseInt(verticesField.getText());
            if (vertices <= 0) {
                errorLabel.setText("Number of vertices must be positive.");
                return;
            }
            if (vertices > 20) {
                errorLabel.setText("Maximum 20 vertices allowed.");
                return;
            }
            initializeGraph(vertices);
            // Add minimal edges for perfect squares to ensure connectivity without overlaps
            if (isPerfectSquare(vertices)) {
                int gridSize = (int) Math.sqrt(vertices);
                // Create a row-major spanning tree with orthogonal edges
                for (int row = 0; row < gridSize; row++) {
                    for (int col = 0; col < gridSize - 1; col++) {
                        int u = row * gridSize + col;
                        int v = row * gridSize + (col + 1);
                        graph.addEdge(u, v, 1 + random.nextInt(10)); // Right
                    }
                    if (row < gridSize - 1) {
                        int u = row * gridSize;
                        int v = (row + 1) * gridSize;
                        graph.addEdge(u, v, 1 + random.nextInt(10)); // Down
                    }
                }
            }
            drawGraph(false);
            errorLabel.setText("Graph created with " + vertices + " vertices.");
            srcField.setDisable(false);
            destField.setDisable(false);
            weightField.setDisable(false);
            addEdgeButton.setDisable(false);
            mstButton.setDisable(false);
            verticesField.clear();
            mstTableView.getItems().clear();
            prevStepButton.setDisable(true);
            nextStepButton.setDisable(true);
            stepIndicatorLabel.setText("");
        } catch (NumberFormatException ex) {
            errorLabel.setText("Please enter a valid integer for vertices.");
        }
    }

    @FXML
    private void addEdge() {
        if (graph == null) {
            errorLabel.setText("Create a graph first.");
            return;
        }
        try {
            int srcLabel = Integer.parseInt(srcField.getText());
            int destLabel = Integer.parseInt(destField.getText());
            int weight = Integer.parseInt(weightField.getText());

            Integer src = reverseLabelMapping.get(srcLabel);
            Integer dest = reverseLabelMapping.get(destLabel);
            if (src == null || dest == null) {
                errorLabel.setText("Invalid vertex labels. Use labels shown in graph.");
                return;
            }
            if (src < 0 || src >= graph.getVertices() || dest < 0 || dest >= graph.getVertices()) {
                errorLabel.setText("Vertices must be valid labels.");
                return;
            }
            if (weight <= 0) {
                errorLabel.setText("Weight must be positive.");
                return;
            }
            if (src == dest) {
                errorLabel.setText("Self-loops are not allowed.");
                return;
            }

            // Check for duplicate edge
            for (Edge edge : graph.getNeighbors(src)) {
                if (edge.getDestination() == dest) {
                    errorLabel.setText("Edge already exists.");
                    return;
                }
            }

            graph.addEdge(src, dest, weight);
            drawGraph(false);
            errorLabel.setText("Edge added: (" + srcLabel + ", " + destLabel + ", " + weight + ")");
            srcField.clear();
            destField.clear();
            weightField.clear();
            mstTableView.getItems().clear();
            prevStepButton.setDisable(true);
            nextStepButton.setDisable(true);
            stepIndicatorLabel.setText("");
        } catch (NumberFormatException ex) {
            errorLabel.setText("Please enter valid integers for source, destination, and weight.");
        }
    }

    @FXML
    private void computeMST() {
        if (graph == null) {
            errorLabel.setText("Create a graph first.");
            return;
        }
        allEdges = graph.getAllEdges();
        if (allEdges.isEmpty()) {
            errorLabel.setText("No edges in the graph.");
            return;
        }
        allEdges.sort(Comparator.comparingInt(e -> e.weight));

        UnionFind uf = new UnionFind(graph.getVertices());
        mstEdges = new ArrayList<>();
        List<MSTStep> mstSteps = new ArrayList<>();
        totalWeight = 0;

        for (WeightedGraph.EdgeInfo edge : allEdges) {
            boolean accepted = uf.union(edge.source, edge.destination);
            if (accepted) {
                mstEdges.add(edge);
                totalWeight += edge.weight;
            }
            String edgeStr = "(" + vertexLabelMapping[edge.source] + ", " + vertexLabelMapping[edge.destination] + ")";
            mstSteps.add(new MSTStep(edgeStr, edge.weight, accepted, totalWeight));
        }

        mstTableView.getItems().setAll(mstSteps);
        currentStep = -1;
        prevStepButton.setDisable(false);
        nextStepButton.setDisable(false);
        stepIndicatorLabel.setText("");
        errorLabel.setText("Ready to start MST. Press Next to begin.");
        updateStep(); // Initialize graph
    }

    @FXML
    private void prevStep() {
        if (currentStep > -1) {
            currentStep--;
            updateStep();
        }
    }

    @FXML
    private void nextStep() {
        if (currentStep < allEdges.size()) {
            currentStep++;
            updateStep();
        }
    }

    private void updateStep() {
        graphPane.getChildren().clear();
        drawGraph(false); // Draw all vertices and edges in black initially
        Set<String> drawnEdges = new HashSet<>();

        // Draw all edges up to current step
        for (int i = 0; i <= currentStep && i < allEdges.size(); i++) {
            WeightedGraph.EdgeInfo edge = allEdges.get(i);
            String edgeKey = Math.min(edge.source, edge.destination) + "-" + Math.max(edge.source, edge.destination);
            if (!drawnEdges.contains(edgeKey)) {
                drawnEdges.add(edgeKey);
                boolean isAccepted = mstEdges.contains(edge);
                drawEdge(edge.source, edge.destination, edge.weight, isAccepted ? Color.LIMEGREEN : Color.RED, edgeKey);
            }
        }

        // Update step indicator and message
        if (currentStep == -1) {
            stepIndicatorLabel.setText("Step 0 of " + allEdges.size());
            errorLabel.setText("Ready to start MST. Press Next to begin.");
            prevStepButton.setDisable(true);
            nextStepButton.setDisable(false);
        } else if (currentStep < allEdges.size()) {
            WeightedGraph.EdgeInfo edge = allEdges.get(currentStep);
            boolean isAccepted = mstEdges.contains(edge);
            stepIndicatorLabel.setText("Step " + (currentStep + 1) + " of " + allEdges.size());
            errorLabel.setText("Considering edge: (" + vertexLabelMapping[edge.source] + ", " + vertexLabelMapping[edge.destination] + ", " + edge.weight + ") - " +
                    (isAccepted ? "Accepted (added to MST)" : "Rejected (forms a cycle)"));
            prevStepButton.setDisable(currentStep == 0);
            nextStepButton.setDisable(currentStep == allEdges.size() - 1);
        } else {
            stepIndicatorLabel.setText("Step " + allEdges.size() + " of " + allEdges.size());
            errorLabel.setText("MST completed with " + mstEdges.size() + " edges, total weight: " + totalWeight);
            prevStepButton.setDisable(false);
            nextStepButton.setDisable(true);
            drawGraph(true); // Show final MST with limegreen edges
        }
    }

    @FXML
    private void clearGraph() {
        graph = null;
        vertexCircles = null;
        vertexLabels = null;
        vertexLabelMapping = null;
        reverseLabelMapping = null;
        vertexPositions = null;
        edgeLines = null;
        edgeWeightLabels = null;
        allEdges = null;
        mstEdges = null;
        currentStep = -1;
        totalWeight = 0;
        graphPane.getChildren().clear();
        verticesField.clear();
        srcField.clear();
        destField.clear();
        weightField.clear();
        errorLabel.setText("");
        stepIndicatorLabel.setText("");
        srcField.setDisable(true);
        destField.setDisable(true);
        weightField.setDisable(true);
        addEdgeButton.setDisable(true);
        mstButton.setDisable(true);
        prevStepButton.setDisable(true);
        nextStepButton.setDisable(true);
        mstTableView.getItems().clear();
    }

    private void initializeGraph(int vertices) {
        graph = new WeightedGraph(vertices);
        vertexLabelMapping = new int[vertices];
        reverseLabelMapping = new HashMap<>();
        List<Integer> labels = new ArrayList<>();
        for (int i = 0; i < vertices; i++) {
            labels.add(i);
        }
        Collections.shuffle(labels, random);
        for (int i = 0; i < vertices; i++) {
            vertexLabelMapping[i] = labels.get(i);
            reverseLabelMapping.put(labels.get(i), i);
        }

        // Initialize vertex positions
        vertexPositions = new ArrayList<>();
        double centerX = graphPane.getPrefWidth() / 2;
        double centerY = graphPane.getPrefHeight() / 2;
        boolean isPerfectSquare = isPerfectSquare(vertices);
        double size = 150; // Half-size for grid or radius for circle
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < vertices; i++) {
            indices.add(i);
        }
        Collections.shuffle(indices, random);

        if (isPerfectSquare) {
            // Grid layout: sqrt(n) x sqrt(n)
            int gridSize = (int) Math.sqrt(vertices);
            double cellSize = 2 * size / gridSize;
            for (int i = 0; i < vertices; i++) {
                int idx = indices.get(i);
                int row = idx / gridSize;
                int col = idx % gridSize;
                double x = centerX - size + col * cellSize + cellSize / 2;
                double y = centerY - size + row * cellSize + cellSize / 2;
                vertexPositions.add(new Point2D(x, y));
            }
        } else {
            // Circular layout
            for (int i = 0; i < vertices; i++) {
                double angle = 2 * Math.PI * indices.get(i) / vertices;
                double x = centerX + size * Math.cos(angle);
                double y = centerY + size * Math.sin(angle);
                vertexPositions.add(new Point2D(x, y));
            }
        }

        edgeLines = new HashMap<>();
        edgeWeightLabels = new HashMap<>();
    }

    private void drawGraph(boolean isMST) {
        graphPane.getChildren().clear();
        edgeLines.clear();
        edgeWeightLabels.clear();
        if (graph == null) return;

        int n = graph.getVertices();
        vertexCircles = new Circle[n];
        vertexLabels = new Text[n];

        // Draw vertices with drag handlers
        for (int i = 0; i < n; i++) {
            double x = vertexPositions.get(i).getX();
            double y = vertexPositions.get(i).getY();

            Circle circle = new Circle(x, y, vertexRadius);
            circle.setFill(Color.LIGHTBLUE);
            circle.setStroke(Color.BLACK);
            vertexCircles[i] = circle;

            Text label = new Text(x - 5, y + 5, String.valueOf(vertexLabelMapping[i]));
            vertexLabels[i] = label;

            // Make vertex draggable
            final int vertexIndex = i;
            circle.setOnMousePressed(event -> {
                dragStartX = event.getSceneX() - circle.getCenterX();
                dragStartY = event.getSceneY() - circle.getCenterY();
                event.consume();
            });
            circle.setOnMouseDragged(event -> {
                double newX = event.getSceneX() - dragStartX;
                double newY = event.getSceneY() - dragStartY;
                // Keep vertex within pane bounds
                newX = Math.max(vertexRadius, Math.min(newX, graphPane.getWidth() - vertexRadius));
                newY = Math.max(vertexRadius, Math.min(newY, graphPane.getHeight() - vertexRadius));
                circle.setCenterX(newX);
                circle.setCenterY(newY);
                label.setX(newX - 5);
                label.setY(newY + 5);
                vertexPositions.set(vertexIndex, new Point2D(newX, newY)); // Update position
                updateEdges(vertexIndex);
                event.consume();
            });

            graphPane.getChildren().addAll(circle, label);
        }

        // Draw edges
        Set<String> drawnEdges = new HashSet<>();
        for (int u = 0; u < n; u++) {
            for (Edge edge : graph.getNeighbors(u)) {
                int v = edge.getDestination();
                String edgeKey = Math.min(u, v) + "-" + Math.max(u, v);
                if (!drawnEdges.contains(edgeKey)) {
                    drawnEdges.add(edgeKey);
                    Color color = isMST && isMstEdge(u, v) ? Color.LIMEGREEN : Color.BLACK;
                    drawEdge(u, v, edge.getWeight(), color, edgeKey);
                }
            }
        }
    }

    private boolean isMstEdge(int u, int v) {
        if (mstEdges == null) return false;
        for (WeightedGraph.EdgeInfo e : mstEdges) {
            if ((e.source == u && e.destination == v) || (e.source == v && e.destination == u)) {
                return true;
            }
        }
        return false;
    }

    private void drawEdge(int u, int v, int weight, Color color, String edgeKey) {
        double x1 = vertexCircles[u].getCenterX();
        double y1 = vertexCircles[u].getCenterY();
        double x2 = vertexCircles[v].getCenterX();
        double y2 = vertexCircles[v].getCenterY();

        double angle = Math.atan2(y2 - y1, x2 - x1);
        double startX = x1 + vertexRadius * Math.cos(angle);
        double startY = y1 + vertexRadius * Math.sin(angle);
        double endX = x2 - vertexRadius * Math.cos(angle);
        double endY = y2 - vertexRadius * Math.sin(angle);

        Line line = new Line(startX, startY, endX, endY);
        line.setStroke(color);
        line.setStrokeWidth(2.0);
        line.setUserData(new int[]{u, v});
        edgeLines.put(edgeKey, line);

        double midX = (startX + endX) / 2;
        double midY = (startY + endY) / 2;
        Text weightLabel = new Text(midX, midY, String.valueOf(weight));
        weightLabel.setFill(Color.RED);
        weightLabel.setUserData(new int[]{u, v});
        edgeWeightLabels.put(edgeKey, weightLabel);

        graphPane.getChildren().addAll(line, weightLabel);
    }

    private void updateEdges(int vertexIndex) {
        List<String> edgesToUpdate = new ArrayList<>();
        for (String edgeKey : edgeLines.keySet()) {
            int[] vertices = (int[]) edgeLines.get(edgeKey).getUserData();
            if (vertices[0] == vertexIndex || vertices[1] == vertexIndex) {
                edgesToUpdate.add(edgeKey);
            }
        }

        for (String edgeKey : edgesToUpdate) {
            int[] vertices = (int[]) edgeLines.get(edgeKey).getUserData();
            int u = vertices[0];
            int v = vertices[1];
            int weight = 0;
            for (Edge edge : graph.getNeighbors(u)) {
                if (edge.getDestination() == v) {
                    weight = edge.getWeight();
                    break;
                }
            }

            double x1 = vertexCircles[u].getCenterX();
            double y1 = vertexCircles[u].getCenterY();
            double x2 = vertexCircles[v].getCenterX();
            double y2 = vertexCircles[v].getCenterY();

            double angle = Math.atan2(y2 - y1, x2 - x1);
            double startX = x1 + vertexRadius * Math.cos(angle);
            double startY = y1 + vertexRadius * Math.sin(angle);
            double endX = x2 - vertexRadius * Math.cos(angle);
            double endY = y2 - vertexRadius * Math.sin(angle);

            Line line = edgeLines.get(edgeKey);
            line.setStartX(startX);
            line.setStartY(startY);
            line.setEndX(endX);
            line.setEndY(endY);

            double midX = (startX + endX) / 2;
            double midY = (startY + endY) / 2;
            Text weightLabel = edgeWeightLabels.get(edgeKey);
            weightLabel.setX(midX);
            weightLabel.setY(midY);
        }
    }

    private boolean isPerfectSquare(int n) {
        int sqrt = (int) Math.sqrt(n);
        return sqrt * sqrt == n;
    }
}
