package toposort.model;

/* Вершина графа: номер и координаты в условной системе 0..1000. */
public class Vertex {
    public final int id;
    public int x;
    public int y;

    public Vertex(int id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }
}
