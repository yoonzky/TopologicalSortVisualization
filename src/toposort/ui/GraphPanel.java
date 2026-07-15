package toposort.ui;

import toposort.model.Edge;
import toposort.model.Graph;
import toposort.model.TopoStepper;
import toposort.model.Vertex;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/*
 * Холст.
 * Клик по пустому месту создаёт вершину, клики по двум вершинам - ребро
 * от первой ко второй, перетаскивание перемещает вершину, правый клик
 * удаляет элемент. Создание и удаление разрешены только до старта
 * алгоритма (до сброса). Перенос вершин разрешён всегда.
 */
public class GraphPanel extends JPanel {

    /* Правки: окно узнаёт, что граф изменился. */
    public interface GraphEditListener {
        void graphEdited(String whatHappened);
    }

    private static final int R = 18;
    private static final Color READY_FILL = new Color(0xFFE082);
    private static final Color DONE_FILL = new Color(0xA5D6A7);
    private static final Color RED = new Color(200, 0, 0);
    private static final Stroke DASH = new BasicStroke(1.5f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER, 10f, new float[]{6f, 6f}, 0f);

    private Graph graph;
    private TopoStepper stepper;
    private GraphEditListener listener;
    private boolean editable = true; // false после старта алгоритма

    private Vertex edgeSource; // выбранное начало будущего ребра
    private Vertex dragged; // перетаскиваемая вершина

    public GraphPanel() {
        setBackground(Color.WHITE);
        MouseAdapter mouse = new Mouse();
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    public void setGraph(Graph g) {
        this.graph = g;
        this.stepper = null;
        this.edgeSource = null;
        repaint();
    }

    public void setStepper(TopoStepper s) {
        this.stepper = s;
        repaint();
    }

    public void setListener(GraphEditListener l) {
        this.listener = l;
    }

    public void setEditable(boolean e) {
        this.editable = e;
        if (!e) edgeSource = null;
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

        if (graph == null || graph.vertexCount() == 0) {
            g2.setColor(Color.GRAY);
            g2.drawString("Загрузите граф (\"Открыть...\", \"Пример\") или создайте:"
                    + " клик - вершина, клики по двум вершинам - ребро", 20, 30);
            if (graph == null) return;
        }

        for (Edge e : graph.edges()) {
            if (stepper != null && stepper.isRemoved(e)) {
                g2.setColor(Color.LIGHT_GRAY);
                g2.setStroke(DASH);
            } else if (stepper != null && stepper.isHighlighted(e)) {
                g2.setColor(RED);
                g2.setStroke(new BasicStroke(3f));
            } else {
                g2.setColor(Color.DARK_GRAY);
                g2.setStroke(new BasicStroke(1.5f));
            }
            Vertex a = graph.findVertex(e.from()), b = graph.findVertex(e.to());
            drawArrow(g2, sx(a.x), sy(a.y), sx(b.x), sy(b.y));
        }

        for (Vertex v : graph.vertices()) drawVertex(g2, v);
    }

    private void drawVertex(Graphics2D g2, Vertex v) {
        int x = sx(v.x), y = sy(v.y);
        TopoStepper.VertexStatus st = (stepper == null)
                ? TopoStepper.VertexStatus.PENDING : stepper.statusOf(v.id);

        Color fill = switch (st) {
            case READY -> READY_FILL;
            case DONE -> DONE_FILL;
            default -> Color.WHITE;
        };
        g2.setColor(fill);
        g2.fillOval(x - R, y - R, 2 * R, 2 * R);

        boolean selected = (v == edgeSource);
        Color ring = st == TopoStepper.VertexStatus.CURRENT
                || st == TopoStepper.VertexStatus.STUCK ? RED
                : selected ? new Color(0, 90, 200) : Color.BLACK;
        g2.setColor(ring);
        g2.setStroke(new BasicStroke(
                st == TopoStepper.VertexStatus.CURRENT || selected ? 3.5f : 2f));
        g2.drawOval(x - R, y - R, 2 * R, 2 * R);

        g2.setColor(Color.BLACK);
        String name = String.valueOf(v.id);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(name, x - fm.stringWidth(name) / 2, y + fm.getAscent() / 2 - 1);

        if (stepper == null) return;
        String label;
        if (st == TopoStepper.VertexStatus.DONE) {
            label = "#" + stepper.orderPosOf(v.id);
            g2.setColor(new Color(0, 120, 0));
        } else {
            label = "in:" + stepper.indegOf(v.id);
            g2.setColor(Color.GRAY);
        }
        g2.drawString(label, x - fm.stringWidth(label) / 2, y - R - 3);
    }

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

    /* Обратное преобразование: пиксели -> условные координаты 0..1000. */
    private int ux(int px) {
        int w = getWidth() - 2 * R;
        return clamp((px - R) * 1000 / Math.max(w, 1));
    }

    private int uy(int py) {
        int h = getHeight() - 2 * R;
        return clamp((py - R) * 1000 / Math.max(h, 1));
    }

    private static int clamp(int c) {
        return Math.max(0, Math.min(1000, c));
    }

    private Vertex vertexAt(int px, int py) {
        for (Vertex v : graph.vertices()) {
            int dx = px - sx(v.x), dy = py - sy(v.y);
            if (dx * dx + dy * dy <= R * R) return v;
        }
        return null;
    }

    private Edge edgeAt(int px, int py) {
        for (Edge e : graph.edges()) {
            Vertex a = graph.findVertex(e.from()), b = graph.findVertex(e.to());
            if (distToSegment(px, py, sx(a.x), sy(a.y), sx(b.x), sy(b.y)) < 6)
                return e;
        }
        return null;
    }

    /* Расстояние от точки до отрезка (в пикселях). */
    private static double distToSegment(int px, int py,
                                        int x1, int y1, int x2, int y2) {
        double dx = x2 - x1, dy = y2 - y1;
        double len2 = dx * dx + dy * dy;
        double t = len2 == 0 ? 0
                : Math.max(0, Math.min(1, ((px - x1) * dx + (py - y1) * dy) / len2));
        double cx = x1 + t * dx, cy = y1 + t * dy;
        return Math.hypot(px - cx, py - cy);
    }

    private class Mouse extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent me) {
            if (graph == null) return;
            if (SwingUtilities.isLeftMouseButton(me))
                dragged = vertexAt(me.getX(), me.getY());
        }

        @Override
        public void mouseDragged(MouseEvent me) {
            if (dragged != null) {              // перенос вершины
                dragged.x = ux(me.getX());
                dragged.y = uy(me.getY());
                repaint();
            }
        }

        @Override
        public void mouseReleased(MouseEvent me) {
            dragged = null;
        }

        @Override
        public void mouseClicked(MouseEvent me) {
            if (graph == null) return;

            if (SwingUtilities.isRightMouseButton(me)) {
                deleteAt(me.getX(), me.getY());
                return;
            }
            if (!SwingUtilities.isLeftMouseButton(me)) return;

            Vertex hit = vertexAt(me.getX(), me.getY());
            if (hit == null) {
                createVertexAt(me.getX(), me.getY());
            } else if (edgeSource == null) {
                edgeSource = hit; // начало будущего ребра
                repaint();
            } else if (edgeSource == hit) {
                edgeSource = null; // снять выбор
                repaint();
            } else {
                createEdge(edgeSource, hit); // ребро от первого ко второму
                edgeSource = null;
            }
        }

        private void createVertexAt(int px, int py) {
            if (!requireEditable()) return;
            try {
                Vertex v = graph.addVertex(graph.nextFreeId(), ux(px), uy(py));
                notifyEdited("Добавлена вершина " + v.id + ".");
            } catch (RuntimeException ex) {
                showError(ex.getMessage());
            }
        }

        private void createEdge(Vertex a, Vertex b) {
            if (!requireEditable()) return;
            try {
                Edge e = graph.addEdge(a.id, b.id);
                notifyEdited("Добавлено ребро " + e + ".");
            } catch (RuntimeException ex) {
                showError(ex.getMessage());
            }
        }

        private void deleteAt(int px, int py) {
            if (!requireEditable()) return;
            Vertex v = vertexAt(px, py);
            if (v != null) {
                long incident = graph.edges().stream()
                        .filter(e -> e.touches(v.id)).count();
                if (incident > 0) {
                    int ans = JOptionPane.showConfirmDialog(GraphPanel.this,
                            "Удалить вершину " + v.id + " вместе с " + incident
                                    + " ребром(ами)?", "Удаление",
                            JOptionPane.YES_NO_OPTION);
                    if (ans != JOptionPane.YES_OPTION) return;
                }
                graph.removeVertex(v.id);
                notifyEdited("Удалена вершина " + v.id + ".");
                return;
            }
            Edge e = edgeAt(px, py);
            if (e != null) {
                graph.removeEdge(e);
                notifyEdited("Удалено ребро " + e + ".");
            }
        }

        private boolean requireEditable() {
            if (editable) return true;
            showError("Во время выполнения алгоритма граф изменять нельзя.\n"
                    + "Нажмите \"Сброс\", чтобы вернуться к редактированию.");
            return false;
        }

        private void notifyEdited(String what) {
            edgeSource = null;
            if (listener != null) listener.graphEdited(what);
            repaint();
        }

        private void showError(String msg) {
            JOptionPane.showMessageDialog(GraphPanel.this, msg,
                    "Нельзя", JOptionPane.WARNING_MESSAGE);
        }
    }
}
