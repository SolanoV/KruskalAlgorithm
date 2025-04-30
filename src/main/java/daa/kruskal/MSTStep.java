package daa.kruskal;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class MSTStep {
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

    public String getEdge() {return edge.get();}
    public SimpleStringProperty edgeProperty() {return edge;}
    public int getWeight() {return weight.get();}
    public SimpleIntegerProperty weightProperty() {return weight;}
    public boolean isAccepted() {return accepted.get();}
    public SimpleBooleanProperty acceptedProperty() {return accepted;}
    public int getTotalWeight() {return totalWeight.get();}
    public SimpleIntegerProperty totalWeightProperty() {return totalWeight;}
}
