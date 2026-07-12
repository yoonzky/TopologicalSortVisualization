package toposort.ui;

import toposort.io.GraphIO;
import toposort.model.Graph;
import toposort.model.TopoSort;

import javax.swing.*;
import java.awt.*;

/*
 * Главное окно: работают "Открыть..." и "Пример";
 * После загрузки сразу выполняется полный прогон алгоритма, результат печатается в журнал и показывается на холсте.
 */
public class MainFrame extends JFrame {

    private final GraphPanel canvas = new GraphPanel();
    private final DefaultListModel<String> stateModel = new DefaultListModel<>();
    private final JTextArea log = new JTextArea(5, 0);

    public MainFrame() {
        super("Визуализатор топологической сортировки");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 640);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(4, 4));

        add(buildTopPanel(), BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);
        add(buildStatePanel(), BorderLayout.EAST);
        add(buildBottomPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildTopPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton open = new JButton("Открыть...");
        open.addActionListener(e -> openFile());
        p.add(open);

        JButton demo = new JButton("Пример");
        demo.addActionListener(e -> loadGraph(Graph.demo(), "встроенный пример"));
        p.add(demo);

        p.add(disabled(new JButton("Сохранить результат...")));
        p.add(disabled(new JButton("Очистить")));
        p.add(Box.createHorizontalStrut(24));
        p.add(disabled(new JButton("О разработчиках")));
        return p;
    }

    private JScrollPane buildStatePanel() {
        JList<String> list = new JList<>(stateModel);
        list.setEnabled(false);
        JScrollPane sp = new JScrollPane(list);
        sp.setPreferredSize(new Dimension(190, 0));
        return sp;
    }

    private JPanel buildBottomPanel() {
        JPanel bottom = new JPanel(new BorderLayout());
        log.setEditable(false);
        log.setText("Загрузите граф: \"Открыть...\" (файл V/E) или \"Пример\".");
        bottom.add(new JScrollPane(log), BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER));
        controls.add(disabled(new JButton("Шаг назад")));
        controls.add(disabled(new JButton("Шаг")));
        controls.add(disabled(new JButton("Автопрогон")));
        controls.add(new JLabel("скорость:"));
        JSlider speed = new JSlider(100, 2000, 700);
        speed.setEnabled(false);
        controls.add(speed);
        controls.add(disabled(new JButton("Сброс")));
        bottom.add(controls, BorderLayout.SOUTH);
        return bottom;
    }

    private void openFile() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            Graph g = GraphIO.load(chooser.getSelectedFile().toPath());
            loadGraph(g, chooser.getSelectedFile().getName());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Не удалось загрузить граф:\n" + ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    /* Показать граф и сразу выполнить полный прогон. */
    private void loadGraph(Graph g, String sourceName) {
        canvas.setGraph(g);
        TopoSort.Result r = TopoSort.run(g);
        canvas.showResult(r);

        stateModel.clear();
        stateModel.addElement("Порядок:");
        for (int i = 0; i < r.order().size(); i++)
            stateModel.addElement("  " + (i + 1) + ") " + r.order().get(i));
        if (r.hasCycle()) {
            stateModel.addElement("");
            stateModel.addElement("В цикле:");
            for (int id : r.remaining()) stateModel.addElement("  " + id);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Граф загружен (").append(sourceName).append("): ")
                .append(g.vertexCount()).append(" вершин, ")
                .append(g.edgeCount()).append(" рёбер.\n");
        if (r.hasCycle()) {
            sb.append("Граф содержит цикл - топологическая сортировка невозможна.\n");
            sb.append("Успели обработать: ").append(r.order()).append('\n');
            sb.append("Необработанные вершины: ").append(r.remaining());
        } else {
            sb.append("Топологический порядок: ");
            for (int id : r.order()) sb.append(id).append(' ');
        }
        log.setText(sb.toString());
    }

    private static JButton disabled(JButton b) {
        b.setEnabled(false);
        return b;
    }
}
