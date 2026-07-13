package toposort.model;

import java.util.ArrayList;
import java.util.List;

/*
 * Ориентированный граф: списки вершин и рёбер.
 * Ограничения (до 50 вершин, до 300 рёбер, без петель и повторных рёбер) проверяются при добавлении.
 * Встречные рёбра u->v и v->u допускаются.
 */
public class Graph {

    public static final int MAX_VERTICES = 50;
    public static final int MAX_EDGES = 300;

    private final List<Vertex> vertices = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();

    public Vertex addVertex(int id, int x, int y) {
        if (vertices.size() >= MAX_VERTICES)
            throw new IllegalStateException("не более " + MAX_VERTICES + " вершин");
        if (findVertex(id) != null)
            throw new IllegalArgumentException("вершина " + id + " уже существует");
        Vertex v = new Vertex(id, x, y);
        vertices.add(v);
        return v;
    }

    public Edge addEdge(int from, int to) {
        if (edges.size() >= MAX_EDGES)
            throw new IllegalStateException("не более " + MAX_EDGES + " рёбер");
        if (from == to)
            throw new IllegalArgumentException("петля (" + from + "->" + to + ") не допускается");
        if (findVertex(from) == null || findVertex(to) == null)
            throw new IllegalArgumentException("ребро " + from + "->" + to + ": такой вершины нет");
        for (Edge e : edges)
            if (e.from() == from && e.to() == to)
                throw new IllegalArgumentException("повторное ребро " + from + "->" + to);
        Edge e = new Edge(from, to);
        edges.add(e);
        return e;
    }

    public Vertex findVertex(int id) {
        for (Vertex v : vertices)
            if (v.id == id) return v;
        return null;
    }

    public List<Vertex> vertices() {
        return vertices;
    }

    public List<Edge> edges() {
        return edges;
    }

    public int vertexCount() {
        return vertices.size();
    }

    public int edgeCount() {
        return edges.size();
    }

    /* Встроенный демонстрационный граф (кнопка "Пример"). */
    public static Graph demo() {
        Graph g = new Graph();
        g.addVertex(1, 100, 150);
        g.addVertex(2, 420, 120);
        g.addVertex(3, 260, 480);
        g.addVertex(4, 700, 300);
        g.addVertex(5, 520, 650);
        g.addVertex(6, 900, 550);
        g.addVertex(7, 820, 120);
        g.addEdge(1, 2);
        g.addEdge(1, 3);
        g.addEdge(3, 2);
        g.addEdge(2, 4);
        g.addEdge(3, 5);
        g.addEdge(5, 4);
        g.addEdge(4, 6);
        g.addEdge(7, 6);
        return g;
    }
}
