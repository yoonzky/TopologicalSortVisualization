package toposort.ui;

import toposort.model.Edge;
import toposort.model.Graph;
import toposort.model.TopoSort;
import toposort.model.Vertex;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class GraphPanel extends JPanel {

    private static final int R = 18;

    private Graph graph;
    private final Map<Integer, Integer> orderPos = new HashMap<>(); // id -> позиция
    private java.util.Set<Integer> remaining = java.util.Set.of();

    public GraphPanel() {
        setBackground(Color.WHITE);
    }

    public void setGraph(Graph g) {
        this.graph = g;
        orderPos.clear();
        remaining = java.util.Set.of();
        repaint();
    }

    /* Показать итог полного прогона */
    public void showResult(TopoSort.Result r) {
        orderPos.clear();
        for (int i = 0; i < r.order().size(); i++)
            orderPos.put(r.order().get(i), i + 1);
        remaining = new java.util.HashSet<>(r.remaining());
        repaint();
    }

    public Graph graph() {
        return graph;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        if (graph == null) {
            g2.setColor(Color.GRAY);
            g2.drawString("Граф не загружен: \"Открыть...\" или \"Пример\"", 20, 30);
            return;
        }

        g2.setColor(Color.DARK_GRAY);
        g2.setStroke(new BasicStroke(1.5f));
        for (Edge e : graph.edges()) {
            Vertex a = graph.findVertex(e.from()), b = graph.findVertex(e.to());
            drawArrow(g2, sx(a.x), sy(a.y), sx(b.x), sy(b.y));
        }

        for (Vertex v : graph.vertices()) drawVertex(g2, v);
    }

    private void drawVertex(Graphics2D g2, Vertex v) {
        int x = sx(v.x), y = sy(v.y);
        boolean done = orderPos.containsKey(v.id);
        boolean stuck = remaining.contains(v.id);

        g2.setColor(done ? new Color(0xA5D6A7) : Color.WHITE);
        g2.fillOval(x - R, y - R, 2 * R, 2 * R);
        g2.setColor(stuck ? new Color(200, 0, 0) : Color.BLACK);
        g2.setStroke(new BasicStroke(stuck ? 3f : 2f));
        g2.drawOval(x - R, y - R, 2 * R, 2 * R);

        g2.setColor(Color.BLACK);
        String name = String.valueOf(v.id);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(name, x - fm.stringWidth(name) / 2, y + fm.getAscent() / 2 - 1);

        if (done) {   // позиция в топологическом порядке - подпись над вершиной
            String pos = "#" + orderPos.get(v.id);
            g2.setColor(new Color(0, 120, 0));
            g2.drawString(pos, x - fm.stringWidth(pos) / 2, y - R - 3);
        }
    }

    /* Линия со стрелкой от края круга-источника до края круга-приёмника. */
    private void drawArrow(Graphics2D g2, int x1, int y1, int x2, int y2) {
        double dx = x2 - x1, dy = y2 - y1;
        double len = Math.hypot(dx, dy);
        if (len < 1) return;
        double ux = dx / len, uy = dy / len;
        int ax = (int) (x1 + ux * R), ay = (int) (y1 + uy * R);
        int bx = (int) (x2 - ux * R), by = (int) (y2 - uy * R);
        g2.drawLine(ax, ay, bx, by);
        double wing = 9;
        int p1x = (int) (bx - ux * wing - uy * wing * 0.5);
        int p1y = (int) (by - uy * wing + ux * wing * 0.5);
        int p2x = (int) (bx - ux * wing + uy * wing * 0.5);
        int p2y = (int) (by - uy * wing - ux * wing * 0.5);
        g2.fillPolygon(new int[]{bx, p1x, p2x}, new int[]{by, p1y, p2y}, 3);
    }

    private int sx(int x) {
        return R + x * (getWidth() - 2 * R) / 1000;
    }

    private int sy(int y) {
        return R + y * (getHeight() - 2 * R) / 1000;
    }
}
