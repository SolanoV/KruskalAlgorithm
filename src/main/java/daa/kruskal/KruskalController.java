package daa.kruskal;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

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
    private Button undoButton;
    @FXML
    private Button redoButton;
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
    private Map<String, Line> edgeLinesBlack; // Stores black background edge lines
    private Map<String, Text> edgeWeightLabelsBlack; // Stores weight labels for black lines
    private Map<String, Text> edgeStatusLabelsBlack; // Stores status labels for black lines
    private Map<String, Line> edgeLinesColored; // Stores colored (limegreen/crimson) edge lines
    private Map<String, Text> edgeWeightLabelsColored; // Stores weight labels for colored lines
    private Map<String, Text> edgeStatusLabelsColored; // Stores status labels for colored lines
    private double dragStartX, dragStartY; // For dragging vertices
    private List<WeightedGraph.EdgeInfo> allEdges; // All edges for MST
    private List<WeightedGraph.EdgeInfo> mstEdges; // MST edges
    private int currentStep; // Current step in MST process
    private int totalWeight; // Total MST weight
    private Stack<GraphAction> undoStack; // Stack for undo actions
    private Stack<GraphAction> redoStack; // Stack for redo actions



    @FXML
    private void initialize() {
        srcField.setDisable(true);
        destField.setDisable(true);
        weightField.setDisable(true);
        addEdgeButton.setDisable(true);
        undoButton.setDisable(true);
        redoButton.setDisable(true);
        mstButton.setDisable(true);
        prevStepButton.setDisable(true);
        nextStepButton.setDisable(true);
        stepIndicatorLabel.setText("");
        undoStack = new Stack<>();
        redoStack = new Stack<>();

        // Enable Enter key to add edge
        srcField.setOnAction(event -> addEdge());
        destField.setOnAction(event -> addEdge());
        weightField.setOnAction(event -> addEdge());

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

        // Add custom cell factory to highlight the current step
        edgeColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String edge, boolean empty) {
                super.updateItem(edge, empty);
                if (empty || edge == null) {
                    setText(null);
                    setStyle("");
                    setFont(Font.font("System", FontWeight.NORMAL, 12));
                } else {
                    setText(edge);
                    int rowIndex = getIndex();
                    if (rowIndex == currentStep && rowIndex >= 0) {
                        setStyle("-fx-background-color: #e0e0e0;");
                        setFont(Font.font("System", FontWeight.BOLD, 12));
                    } else {
                        setStyle("");
                        setFont(Font.font("System", FontWeight.NORMAL, 12));
                    }
                }
            }
        });
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
            // Record current graph state for undo
            GraphAction action = new GraphAction("create", graph != null ? graph.getVertices() : 0,
                    graph != null ? graph.getAllEdges() : null, vertexPositions, vertexLabelMapping, reverseLabelMapping);
            undoStack.push(action);
            redoStack.clear();
            initializeGraph(vertices);
            drawGraph(false);
            errorLabel.setText("Graph created with " + vertices + " vertices.");
            srcField.setDisable(false);
            destField.setDisable(false);
            weightField.setDisable(false);
            addEdgeButton.setDisable(false);
            undoButton.setDisable(false);
            redoButton.setDisable(true);
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
            int srcLabel = Integer.parseInt(srcField.getText())-1;
            int destLabel = Integer.parseInt(destField.getText())-1;
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
            // Record action for undo
            undoStack.push(new GraphAction("add", src, dest, weight));
            redoStack.clear();
            undoButton.setDisable(false);
            redoButton.setDisable(true);
            drawGraph(false);
            errorLabel.setText("Edge added: (" + (srcLabel+1) + ", " + (destLabel+1) + ", " + weight + ")");
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
    private void undo() {
        if (!undoStack.isEmpty()) {
            GraphAction action = undoStack.pop();
            if (action.type.equals("add")) {
                // Undo adding an edge
                graph.removeEdge(action.source, action.destination);
                redoStack.push(new GraphAction("remove", action.source, action.destination, action.weight));
                errorLabel.setText("Undone: Added edge (" + vertexLabelMapping[action.source] + ", " +
                        vertexLabelMapping[action.destination] + ")");
                drawGraph(false);
            } else if (action.type.equals("create")) {
                // Undo creating a graph
                redoStack.push(new GraphAction("create", graph != null ? graph.getVertices() : 0,
                        graph != null ? graph.getAllEdges() : null, vertexPositions, vertexLabelMapping, reverseLabelMapping));
                if (action.vertices > 0) {
                    // Restore previous graph
                    graph = new WeightedGraph(action.vertices);
                    vertexPositions = action.positions;
                    vertexLabelMapping = action.labelMapping;
                    reverseLabelMapping = action.reverseMapping;
                    for (WeightedGraph.EdgeInfo edge : action.edges) {
                        graph.addEdge(edge.source, edge.destination, edge.weight);
                    }
                    drawGraph(false);
                    errorLabel.setText("Undone: Created graph with " + action.vertices + " vertices");
                    srcField.setDisable(false);
                    destField.setDisable(false);
                    weightField.setDisable(false);
                    addEdgeButton.setDisable(false);
                    mstButton.setDisable(false);
                } else {
                    // No previous graph, clear everything
                    clearGraphInternal();
                    errorLabel.setText("Undone: Created graph");
                }
            } else if (action.type.equals("clear")) {
                // Undo clearing a graph
                redoStack.push(new GraphAction("clear", 0, null, null, null, null));
                graph = new WeightedGraph(action.vertices);
                vertexPositions = action.positions;
                vertexLabelMapping = action.labelMapping;
                reverseLabelMapping = action.reverseMapping;
                for (WeightedGraph.EdgeInfo edge : action.edges) {
                    graph.addEdge(edge.source, edge.destination, edge.weight);
                }
                drawGraph(false);
                errorLabel.setText("Undone: Cleared graph");
                srcField.setDisable(false);
                destField.setDisable(false);
                weightField.setDisable(false);
                addEdgeButton.setDisable(false);
                mstButton.setDisable(false);
            }
            mstTableView.getItems().clear();
            prevStepButton.setDisable(true);
            nextStepButton.setDisable(true);
            stepIndicatorLabel.setText("");
            undoButton.setDisable(undoStack.isEmpty());
            redoButton.setDisable(redoStack.isEmpty());
        }
    }

    @FXML
    private void redo() {
        if (!redoStack.isEmpty()) {
            GraphAction action = redoStack.pop();
            if (action.type.equals("remove")) {
                // Redo adding an edge
                graph.addEdge(action.source, action.destination, action.weight);
                undoStack.push(new GraphAction("add", action.source, action.destination, action.weight));
                errorLabel.setText("Redone: Added edge (" + vertexLabelMapping[action.source] + ", " +
                        vertexLabelMapping[action.destination] + ")");
                drawGraph(false);
            } else if (action.type.equals("create")) {
                // Redo creating a graph
                undoStack.push(new GraphAction("create", graph != null ? graph.getVertices() : 0,
                        graph != null ? graph.getAllEdges() : null, vertexPositions, vertexLabelMapping, reverseLabelMapping));
                initializeGraph(action.vertices);
                for (WeightedGraph.EdgeInfo edge : action.edges) {
                    graph.addEdge(edge.source, edge.destination, edge.weight);
                }
                vertexPositions = action.positions;
                vertexLabelMapping = action.labelMapping;
                reverseLabelMapping = action.reverseMapping;
                drawGraph(false);
                errorLabel.setText("Redone: Created graph with " + action.vertices + " vertices");
                srcField.setDisable(false);
                destField.setDisable(false);
                weightField.setDisable(false);
                addEdgeButton.setDisable(false);
                mstButton.setDisable(false);
            } else if (action.type.equals("clear")) {
                // Redo clearing a graph
                undoStack.push(new GraphAction("clear", graph != null ? graph.getVertices() : 0,
                        graph != null ? graph.getAllEdges() : null, vertexPositions, vertexLabelMapping, reverseLabelMapping));
                clearGraphInternal();
                errorLabel.setText("Redone: Cleared graph");
            }
            mstTableView.getItems().clear();
            prevStepButton.setDisable(true);
            nextStepButton.setDisable(true);
            stepIndicatorLabel.setText("");
            undoButton.setDisable(undoStack.isEmpty());
            redoButton.setDisable(redoStack.isEmpty());
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
        // Do not reinitialize the maps; reuse them to keep references consistent
        edgeLinesBlack.clear();
        edgeWeightLabelsBlack.clear();
        edgeStatusLabelsBlack.clear();
        edgeLinesColored.clear();
        edgeWeightLabelsColored.clear();
        edgeStatusLabelsColored.clear();

        // Draw vertices first to ensure they are below edges
        drawVertices();

        // Draw all edges in black (background edges)
        Set<String> drawnEdges = new HashSet<>();
        for (int u = 0; u < graph.getVertices(); u++) {
            for (Edge edge : graph.getNeighbors(u)) {
                int v = edge.getDestination();
                String edgeKey = Math.min(u, v) + "-" + Math.max(u, v);
                if (!drawnEdges.contains(edgeKey)) {
                    drawnEdges.add(edgeKey);
                    drawEdge(u, v, edge.getWeight(), Color.BLACK, edgeKey, false, true);
                }
            }
        }

        // Draw MST edges up to current step in limegreen or crimson
        drawnEdges.clear();
        for (int i = 0; i <= currentStep && i < allEdges.size(); i++) {
            WeightedGraph.EdgeInfo edge = allEdges.get(i);
            String edgeKey = Math.min(edge.source, edge.destination) + "-" + Math.max(edge.source, edge.destination);
            if (!drawnEdges.contains(edgeKey)) {
                drawnEdges.add(edgeKey);
                boolean isAccepted = mstEdges.contains(edge);
                drawEdge(edge.source, edge.destination, edge.weight, isAccepted ? Color.LIMEGREEN : Color.CRIMSON, edgeKey, isAccepted, false);
            }
        }

        // Update TableView to show steps up to current step
        List<MSTStep> visibleSteps = new ArrayList<>();
        for (int i = 0; i <= Math.max(0, currentStep) && i < allEdges.size(); i++) {
            WeightedGraph.EdgeInfo edge = allEdges.get(i);
            boolean isAccepted = mstEdges.contains(edge);
            String edgeStr = "(" + (vertexLabelMapping[edge.source]+1) + ", " + (vertexLabelMapping[edge.destination]+1) + ")";

            // Calculate total weight by summing weights of MST edges up to this step
            int stepTotalWeight = 0;
            for (int j = 0; j <= i; j++) {
                WeightedGraph.EdgeInfo priorEdge = allEdges.get(j);
                if (mstEdges.contains(priorEdge)) {
                    stepTotalWeight += priorEdge.weight;
                }
            }

            visibleSteps.add(new MSTStep(edgeStr, edge.weight, isAccepted, stepTotalWeight));
        }
        mstTableView.getItems().setAll(visibleSteps);

        // Update step indicator and message
        if (currentStep == -1) {
            stepIndicatorLabel.setText("Step 0 of " + allEdges.size());
            errorLabel.setText("Ready to start MST. Press Next to begin.");
            prevStepButton.setDisable(true);
            nextStepButton.setDisable(false);
            mstTableView.getItems().clear(); // Clear table at step -1
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
            drawGraph(true); // Show final MST with black non-MST edges and limegreen MST edges
        }

        // Refresh TableView to apply styling
        mstTableView.refresh();
    }

    @FXML
    private void clearGraph() {
        if (graph != null) {
            // Record current graph state for undo
            undoStack.push(new GraphAction("clear", graph.getVertices(), graph.getAllEdges(),
                    vertexPositions, vertexLabelMapping, reverseLabelMapping));
            redoStack.clear();
            undoButton.setDisable(false);
            redoButton.setDisable(true);
        }
        clearGraphInternal();
        errorLabel.setText("Graph cleared.");
    }

    private void clearGraphInternal() {
        graph = null;
        vertexCircles = null;
        vertexLabels = null;
        vertexLabelMapping = null;
        reverseLabelMapping = null;
        vertexPositions = null;
        edgeLinesBlack = null;
        edgeWeightLabelsBlack = null;
        edgeStatusLabelsBlack = null;
        edgeLinesColored = null;
        edgeWeightLabelsColored = null;
        edgeStatusLabelsColored = null;
        allEdges = null;
        mstEdges = null;
        currentStep = -1;
        totalWeight = 0;
        graphPane.getChildren().clear();
        verticesField.clear();
        srcField.clear();
        destField.clear();
        weightField.clear();
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

        edgeLinesBlack = new HashMap<>();
        edgeWeightLabelsBlack = new HashMap<>();
        edgeStatusLabelsBlack = new HashMap<>();
        edgeLinesColored = new HashMap<>();
        edgeWeightLabelsColored = new HashMap<>();
        edgeStatusLabelsColored = new HashMap<>();
    }

    private void drawGraph(boolean isMST) {
        graphPane.getChildren().clear();
        edgeLinesBlack = new HashMap<>();
        edgeWeightLabelsBlack = new HashMap<>();
        edgeStatusLabelsBlack = new HashMap<>();
        edgeLinesColored = new HashMap<>();
        edgeWeightLabelsColored = new HashMap<>();
        edgeStatusLabelsColored = new HashMap<>();
        if (graph == null) return;

        drawVertices();

        // Draw all edges in black initially with crimson weight labels
        Set<String> drawnEdges = new HashSet<>();
        for (int u = 0; u < graph.getVertices(); u++) {
            for (Edge edge : graph.getNeighbors(u)) {
                int v = edge.getDestination();
                String edgeKey = Math.min(u, v) + "-" + Math.max(u, v);
                if (!drawnEdges.contains(edgeKey)) {
                    drawnEdges.add(edgeKey);
                    drawEdge(u, v, edge.getWeight(), Color.BLACK, edgeKey, false, true);
                }
            }
        }

        // If in MST mode, update the color of MST edges and their weight labels to limegreen
        if (isMST) {
            for (WeightedGraph.EdgeInfo edge : mstEdges) {
                int u = edge.source;
                int v = edge.destination;
                String edgeKey = Math.min(u, v) + "-" + Math.max(u, v);
                // Update the existing line color to limegreen
                Line line = edgeLinesBlack.get(edgeKey);
                if (line != null) {
                    line.setStroke(Color.LIMEGREEN);
                }
                // Update the existing weight label color to limegreen
                Text weightLabel = edgeWeightLabelsBlack.get(edgeKey);
                if (weightLabel != null) {
                    weightLabel.setFill(Color.LIMEGREEN);
                }
                // Ensure no check/X symbol in final MST view
                Text statusLabel = edgeStatusLabelsBlack.get(edgeKey);
                if (statusLabel != null) {
                    statusLabel.setText("");
                }
            }
        }
    }

    private void drawVertices() {
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

            // Vertex label offset
            Text label = new Text(x - 5, y + 5, String.valueOf(vertexLabelMapping[i]+1));
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

    private void drawEdge(int u, int v, int weight, Color color, String edgeKey, boolean isAccepted, boolean isBlack) {
        double x1 = vertexCircles[u].getCenterX();
        double y1 = vertexCircles[u].getCenterY();
        double x2 = vertexCircles[v].getCenterX();
        double y2 = vertexCircles[v].getCenterY();

        // Ensure edges connect to vertex boundaries
        double angle = Math.atan2(y2 - y1, x2 - x1);
        double startX = x1 + vertexRadius * Math.cos(angle);
        double startY = y1 + vertexRadius * Math.sin(angle);
        double endX = x2 - vertexRadius * Math.cos(angle);
        double endY = y2 - vertexRadius * Math.sin(angle);

        Line line = new Line(startX, startY, endX, endY);
        line.setStroke(color);
        line.setStrokeWidth(2.0);
        line.setUserData(new int[]{u, v});
        if (isBlack) {
            edgeLinesBlack.put(edgeKey, line);
        } else {
            edgeLinesColored.put(edgeKey, line);
        }

        double midX = (startX + endX) / 2;
        double midY = (startY + endY) / 2;
        // Horizontal offset for weight label
        Text weightLabel = new Text(midX - 10, midY, String.valueOf(weight));
        weightLabel.setFill(isBlack ? Color.CRIMSON : color); // Crimson for black lines, matching color for MST lines
        weightLabel.setUserData(new int[]{u, v});
        if (isBlack) {
            edgeWeightLabelsBlack.put(edgeKey, weightLabel);
        } else {
            edgeWeightLabelsColored.put(edgeKey, weightLabel);
        }

        // Add check/X symbol for MST edges during step-by-step
        Text statusLabel = new Text(midX + 5, midY, "");
        if (!isBlack) { // Only for colored lines in step-by-step view
            if (color == Color.LIMEGREEN && !isFinalMST()) {
                statusLabel.setText("\u2714"); // Check mark
                statusLabel.setFill(Color.LIMEGREEN);
            } else if (color == Color.CRIMSON) {
                statusLabel.setText("\u2717"); // X mark
                statusLabel.setFill(Color.CRIMSON);
            }
        }
        statusLabel.setUserData(new int[]{u, v});
        if (isBlack) {
            edgeStatusLabelsBlack.put(edgeKey, statusLabel);
        } else {
            edgeStatusLabelsColored.put(edgeKey, statusLabel);
        }

        graphPane.getChildren().addAll(line, weightLabel, statusLabel);
    }

    private boolean isFinalMST() {
        return currentStep >= allEdges.size();
    }

    private void updateEdges(int vertexIndex) {
        // Collect edges that need updating (involving the dragged vertex)
        List<String> edgesToUpdate = new ArrayList<>();
        // Check black lines
        for (String edgeKey : edgeLinesBlack.keySet()) {
            int[] vertices = (int[]) edgeLinesBlack.get(edgeKey).getUserData();
            if (vertices[0] == vertexIndex || vertices[1] == vertexIndex) {
                edgesToUpdate.add(edgeKey);
            }
        }
        // Check colored lines (won't have duplicates since drawnEdges prevents it)
        for (String edgeKey : edgeLinesColored.keySet()) {
            int[] vertices = (int[]) edgeLinesColored.get(edgeKey).getUserData();
            if (vertices[0] == vertexIndex || vertices[1] == vertexIndex) {
                edgesToUpdate.add(edgeKey);
            }
        }

        // Update all relevant edges
        for (String edgeKey : edgesToUpdate) {
            // Get the vertices for this edge
            int[] vertices;
            Line lineBlack = edgeLinesBlack.get(edgeKey);
            if (lineBlack != null) {
                vertices = (int[]) lineBlack.getUserData();
            } else {
                // If not in black, must be in colored
                vertices = (int[]) edgeLinesColored.get(edgeKey).getUserData();
            }
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

            double midX = (startX + endX) / 2;
            double midY = (startY + endY) / 2;

            // Update black line and its labels
            if (lineBlack != null) {
                lineBlack.setStartX(startX);
                lineBlack.setStartY(startY);
                lineBlack.setEndX(endX);
                lineBlack.setEndY(endY);

                Text weightLabelBlack = edgeWeightLabelsBlack.get(edgeKey);
                if (weightLabelBlack != null) {
                    weightLabelBlack.setX(midX - 10);
                    weightLabelBlack.setY(midY);
                }

                Text statusLabelBlack = edgeStatusLabelsBlack.get(edgeKey);
                if (statusLabelBlack != null) {
                    statusLabelBlack.setX(midX + 5);
                    statusLabelBlack.setY(midY);
                    // In drawGraph (final MST view), status labels are cleared
                    // No need to update check/X here since drawGraph handles it
                }
            }

            // Update colored line and its labels (if it exists)
            Line lineColored = edgeLinesColored.get(edgeKey);
            if (lineColored != null) {
                lineColored.setStartX(startX);
                lineColored.setStartY(startY);
                lineColored.setEndX(endX);
                lineColored.setEndY(endY);

                Text weightLabelColored = edgeWeightLabelsColored.get(edgeKey);
                if (weightLabelColored != null) {
                    weightLabelColored.setX(midX - 10);
                    weightLabelColored.setY(midY);
                }

                Text statusLabelColored = edgeStatusLabelsColored.get(edgeKey);
                if (statusLabelColored != null) {
                    statusLabelColored.setX(midX + 5);
                    statusLabelColored.setY(midY);
                    if (lineColored.getStroke() == Color.LIMEGREEN && !isFinalMST()) {
                        statusLabelColored.setText("\u2714");
                        statusLabelColored.setFill(Color.LIMEGREEN);
                    } else if (lineColored.getStroke() == Color.CRIMSON) {
                        statusLabelColored.setText("\u2717");
                        statusLabelColored.setFill(Color.CRIMSON);
                    } else {
                        statusLabelColored.setText("");
                    }
                }
            }
        }
    }

    private boolean isPerfectSquare(int n) {
        int sqrt = (int) Math.sqrt(n);
        return sqrt * sqrt == n;
    }
}
