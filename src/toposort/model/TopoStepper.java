package toposort.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * Пошаговая топологическая сортировка.
 * Одна вершина обрабатывается за два шага: сначала берётся из очереди
 * готовых (подсвечивается вместе с исходящими рёбрами), затем её рёбра
 * удаляются и степени соседей уменьшаются. Плюс стартовый шаг с подсчётом
 * степеней и итоговый шаг.
 * Реализовано конечным автоматом: фаза хранится в поле phase, stepForward
 * делает один шаг и возвращает текст пояснения для журнала.
 * Из готовых всегда берётся наименьший номер - результат детерминирован.
 * Шаг назад сделан через reset и повтор на один шаг меньше: благодаря
 * детерминированности состояние восстанавливается точно, а при наших
 * размерах графа повтор мгновенен.
 */
public class TopoStepper {

    /* Состояние вершины для раскраски на холсте. */
    public enum VertexStatus {PENDING, READY, CURRENT, DONE, STUCK}

    private enum Phase {START, TAKE, REMOVE, SUMMARY, DONE}

    private final Graph graph;

    private Map<Integer, Integer> indeg;
    private final List<Integer> ready = new ArrayList<>();
    private final List<Integer> order = new ArrayList<>();
    private final Set<Edge> removed = new HashSet<>();
    private final Set<Integer> stuck = new HashSet<>();
    private int current; // вершина между шагами TAKE и REMOVE, иначе -1
    private Phase phase;
    private int stepNo;
    private int callCount; // сделано шагов (для stepBack)

    public TopoStepper(Graph graph) {
        this.graph = graph;
        reset();
    }

    /* Вернуть алгоритм к началу (граф не трогается). */
    public final void reset() {
        indeg = new HashMap<>();
        for (Vertex v : graph.vertices()) indeg.put(v.id, 0);
        for (Edge e : graph.edges()) indeg.merge(e.to(), 1, Integer::sum);
        ready.clear();
        for (Vertex v : graph.vertices())
            if (indeg.get(v.id) == 0) ready.add(v.id);
        Collections.sort(ready);
        order.clear();
        removed.clear();
        stuck.clear();
        current = -1;
        phase = Phase.START;
        stepNo = 0;
        callCount = 0;
    }

    /* Было ли начато выполнение (для блокировки редактирования графа). */
    public boolean isStarted() {
        return callCount > 0;
    }

    /*
     * Шаг назад: сброс и повтор на один шаг меньше.
     * Возвращает пояснение последнего оставшегося шага или null.
     */
    public String stepBack() {
        if (callCount == 0) return null;
        int target = callCount - 1;
        reset();
        String last = null;
        while (callCount < target) last = stepForward();
        return last;
    }

    public boolean isFinished() {
        return phase == Phase.DONE;
    }

    /* Один шаг вперёд; возвращает текст пояснения для журнала. */
    public String stepForward() {
        if (phase != Phase.DONE) callCount++;
        switch (phase) {
            case START -> {
                phase = ready.isEmpty() ? Phase.SUMMARY : Phase.TAKE;
                return "Старт. Входящие степени посчитаны. В очереди готовых "
                        + "(in = 0): " + ready + ".";
            }
            case TAKE -> {
                current = ready.remove(0);      // наименьший из готовых
                stepNo++;
                phase = Phase.REMOVE;
                return "Шаг " + stepNo + ". Берём из очереди вершину " + current
                        + " - все её зависимости выполнены.";
            }
            case REMOVE -> {
                List<Integer> released = new ArrayList<>();
                int cut = 0;
                for (Edge e : graph.edges()) {
                    if (e.from() != current || removed.contains(e)) continue;
                    removed.add(e);
                    cut++;
                    int d = indeg.merge(e.to(), -1, Integer::sum);
                    if (d == 0) released.add(e.to());
                }
                ready.addAll(released);
                Collections.sort(ready);
                order.add(current);
                String msg = "Вершина " + current + " получает номер #" + order.size()
                        + ". Удалено исходящих рёбер: " + cut + "."
                        + (released.isEmpty() ? " Новых готовых вершин нет."
                        : " Стали готовы и добавлены в очередь: " + released + ".");
                current = -1;
                phase = ready.isEmpty() ? Phase.SUMMARY : Phase.TAKE;
                return msg;
            }
            case SUMMARY -> {
                phase = Phase.DONE;
                return summary();
            }
            default -> {
                return "Алгоритм завершён (нажмите \"Сброс\").";
            }
        }
    }

    /* Итоговое сообщение; заодно помечает вершины цикла. */
    public String summary() {
        if (order.size() == graph.vertexCount()) {
            StringBuilder sb = new StringBuilder("Итог: топологический порядок построен: ");
            for (int id : order) sb.append(id).append(' ');
            return sb.toString().trim() + ".";
        }
        stuck.clear();
        for (Vertex v : graph.vertices())
            if (!order.contains(v.id)) stuck.add(v.id);
        return "Итог: очередь пуста, но остались вершины " + sortedStuck()
                + " - граф содержит цикл, топологическая сортировка невозможна.";
    }

    private List<Integer> sortedStuck() {
        List<Integer> s = new ArrayList<>(stuck);
        Collections.sort(s);
        return s;
    }

    // доступ для интерфейса (только чтение)

    public VertexStatus statusOf(int id) {
        if (id == current) return VertexStatus.CURRENT;
        if (order.contains(id)) return VertexStatus.DONE;
        if (stuck.contains(id)) return VertexStatus.STUCK;
        if (ready.contains(id)) return VertexStatus.READY;
        return VertexStatus.PENDING;
    }

    public int indegOf(int id) {
        return indeg.getOrDefault(id, 0);
    }

    /* Позиция вершины в порядке (1-based) или -1. */
    public int orderPosOf(int id) {
        int i = order.indexOf(id);
        return i < 0 ? -1 : i + 1;
    }

    public boolean isRemoved(Edge e) {
        return removed.contains(e);
    }

    /* Рёбра текущей вершины подсвечиваются между шагами TAKE и REMOVE. */
    public boolean isHighlighted(Edge e) {
        return current != -1 && e.from() == current && !removed.contains(e);
    }

    public List<Integer> readyList() {
        return ready;
    }

    public List<Integer> orderList() {
        return order;
    }

    public Set<Integer> stuckSet() {
        return stuck;
    }
}
