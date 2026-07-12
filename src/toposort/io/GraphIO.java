package toposort.io;

import toposort.model.Graph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/*
 * Чтение графа из текстового файла.
 * Ошибка формата - GraphFormatException с номером строки.
 */
public final class GraphIO {

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

    private static void require(boolean cond, int line, String msg)
            throws GraphFormatException {
        if (!cond) throw new GraphFormatException(line, msg);
    }
}
