package toposort.io;

import toposort.model.Edge;
import toposort.model.Graph;
import toposort.model.TopoSort;
import toposort.model.Vertex;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/*
 * Чтение графа из текстового файла и запись результата сортировки в файл.
 * Ошибка формата - GraphFormatException с номером строки.
 */
public final class GraphIO {

    /* Первая строка файла результата - по ней такой файл распознаётся при открытии. */
    public static final String RESULT_MARKER = "# Топологическая сортировка (алгоритм Кана)";

    public static class GraphFormatException extends Exception {
        public GraphFormatException(int line, String msg) {
            super("строка " + line + ": " + msg);
        }
    }

    private GraphIO() {
    }

    public static Graph load(Path file) throws IOException, GraphFormatException {
        List<String> lines = Files.readAllLines(file);
        Graph g = new Graph();
        for (int i = 0; i < lines.size(); i++) {
            int lineNo = i + 1;
            String line = lines.get(i).trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] t = line.split("\\s+");
            try {
                switch (t[0]) {
                    case "V" -> {
                        require(t.length == 4, lineNo, "ожидается: V <номер> <x> <y>");
                        int id = Integer.parseInt(t[1]);
                        int x = Integer.parseInt(t[2]);
                        int y = Integer.parseInt(t[3]);
                        require(id >= 1, lineNo, "номер вершины должен быть >= 1");
                        require(x >= 0 && x <= 1000 && y >= 0 && y <= 1000,
                                lineNo, "координаты должны быть в 0..1000");
                        g.addVertex(id, x, y);
                    }
                    case "E" -> {
                        require(t.length == 3, lineNo, "ожидается: E <из> <в>");
                        g.addEdge(Integer.parseInt(t[1]), Integer.parseInt(t[2]));
                    }
                    default -> throw new GraphFormatException(lineNo,
                            "неизвестная запись \"" + t[0] + "\" (ожидается V или E)");
                }
            } catch (NumberFormatException e) {
                throw new GraphFormatException(lineNo, "не число: " + e.getMessage());
            } catch (IllegalArgumentException | IllegalStateException e) {
                throw new GraphFormatException(lineNo, e.getMessage());
            }
        }
        if (g.vertexCount() == 0)
            throw new GraphFormatException(lines.size(), "в файле нет ни одной вершины");
        return g;
    }

    /*
     * Записать результат сортировки в текстовый файл.
     * Итог пишется строками-комментариями (#), а ниже сохраняется сам граф
     * в обычном формате V/E, причём вершины идут в топологическом порядке.
     * Такой файл можно открыть в программе как входной граф и сразу
     * получить его же, отсортированный топологической сортировкой.
     */
    public static void saveResult(Path file, Graph g, TopoSort.Result r) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(RESULT_MARKER).append('\n');
        sb.append("# Вершин: ").append(g.vertexCount()).append(", рёбер: ").append(g.edgeCount()).append('\n');
        if (r.hasCycle()) {
            sb.append("# Граф содержит цикл - сортировка невозможна.\n");
            sb.append("# Успели обработать: ").append(r.order()).append('\n');
            sb.append("# Необработанные вершины: ").append(r.remaining()).append('\n');
        } else {
            sb.append("# Топологический порядок:");
            for (int id : r.order()) sb.append(' ').append(id);
            sb.append('\n');
            sb.append("# Вершины ниже записаны в этом порядке.\n");
        }

        // сам граф: вершины в топологическом порядке (при цикле - сначала
        // обработанные, затем оставшиеся), потом все рёбра
        for (int id : r.order()) {
            Vertex v = g.findVertex(id);
            sb.append("V ").append(v.id).append(' ').append(v.x).append(' ').append(v.y).append('\n');
        }
        for (int id : r.remaining()) {
            Vertex v = g.findVertex(id);
            sb.append("V ").append(v.id).append(' ').append(v.x).append(' ').append(v.y).append('\n');
        }
        for (Edge e : g.edges()) {
            sb.append("E ").append(e.from()).append(' ').append(e.to()).append('\n');
        }
        Files.writeString(file, sb.toString());
    }

    /* Файл был сохранён этой программой как результат сортировки? */
    public static boolean isResultFile(Path file) {
        try {
            List<String> lines = Files.readAllLines(file);
            return !lines.isEmpty() && lines.get(0).trim().equals(RESULT_MARKER);
        } catch (IOException e) {
            return false;
        }
    }

    private static void require(boolean cond, int line, String msg)
            throws GraphFormatException {
        if (!cond) throw new GraphFormatException(line, msg);
    }
}
