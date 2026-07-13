package toposort.model;

/*
 * Ориентированное ребро from -> to. Record: конструктор, геттеры,
 * equals/hashCode генерируются автоматически.
 */
public record Edge(int from, int to) {

    public boolean touches(int id) {
        return from == id || to == id;
    }

    @Override
    public String toString() {
        return from + "->" + to;
    }
}
