package toposort.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Топологическая сортировка, полный прогон (алгоритм Кана).
 * 1) считаем входящие степени;
 * 2) вершины со степенью 0 - в очередь готовых;
 * 3) берём из очереди наименьший номер, дописываем в порядок, удаляем его исходящие рёбра, новые нули добавляем в очередь;
 * 4) если вершины остались, а очередь пуста - в графе цикл.
 * Из готовых всегда берётся наименьший номер, поэтому результат детерминирован.
 */
public final class TopoSort {

    /* Результат: порядок и вершины, не вошедшие в него (пусто, если цикла нет). */
    public record Result(List<Integer> order, List<Integer> remaining) {
        public boolean hasCycle() {
            return !remaining.isEmpty();
        }
    }

    private TopoSort() {
    }

    public static Result run(Graph g) {
        Map<Integer, Integer> indeg = new HashMap<>();
        for (Vertex v : g.vertices()) indeg.put(v.id, 0);
        for (Edge e : g.edges()) indeg.merge(e.to(), 1, Integer::sum);

        List<Integer> ready = new ArrayList<>();
        for (Vertex v : g.vertices())
            if (indeg.get(v.id) == 0) ready.add(v.id);
        Collections.sort(ready);

        List<Integer> order = new ArrayList<>();
        while (!ready.isEmpty()) {
            int v = ready.remove(0); // наименьший из готовых
            order.add(v);
            for (Edge e : g.edges()) {
                if (e.from() != v) continue;
                int d = indeg.merge(e.to(), -1, Integer::sum);
                if (d == 0) ready.add(e.to());
            }
            Collections.sort(ready);
        }

        List<Integer> remaining = new ArrayList<>();
        for (Vertex v : g.vertices())
            if (!order.contains(v.id)) remaining.add(v.id);
        Collections.sort(remaining);
        return new Result(order, remaining);
    }
}
