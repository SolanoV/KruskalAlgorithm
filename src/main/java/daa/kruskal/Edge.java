package daa.kruskal;

public class Edge {
    private int destination;
    private int weight;

    Edge(){
        this.destination=-1;
        this.weight=-1;
    }
    Edge(int destination, int weight) {
        this.destination = destination;
        this.weight = weight;
    }

    public int getDestination() {
        return destination;
    }
    public void setDestination(int destination) {
        this.destination = destination;
    }
    public int getWeight() {
        return weight;
    }
    public void setWeight(int weight) {
        this.weight = weight;
    }
    @Override
    public String toString() {
        return "Edge [destination=" + destination + ", weight=" + weight + "]";
    }
}
