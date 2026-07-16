package toposort.ui;

import toposort.io.GraphIO;
import toposort.model.Graph;
import toposort.model.TopoSort;
import toposort.model.TopoStepper;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.image.BufferedImage;

/*
 * Главное окно.
 */
public class MainFrame extends JFrame implements GraphPanel.GraphEditListener {

    private final GraphPanel canvas = new GraphPanel();
    private final DefaultListModel<String> stateModel = new DefaultListModel<>();
    private final JTextArea log = new JTextArea(6, 0);
    private final JLabel statusBar = new JLabel(" ");

    private TopoStepper stepper;
    private JButton stepBtn, backBtn, autoBtn, resetBtn, saveBtn;
    private JSlider speed;
    private Timer autoTimer; // javax.swing.Timer - его обработчик работает в EDT

    public MainFrame() {
        super("Визуализатор топологической сортировки");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 680);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(4, 4));

        add(buildTopPanel(), BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);
        add(buildStatePanel(), BorderLayout.EAST);
        add(buildBottomPanel(), BorderLayout.SOUTH);

        canvas.setListener(this);
        // пустой холст, можно сразу рисовать граф мышью
        loadGraph(new Graph(), "пустой холст");
    }

    private JPanel buildTopPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton open = new JButton("Открыть");
        open.addActionListener(e -> openFile());
        p.add(open);

        JButton demo = new JButton("Пример");
        demo.addActionListener(e -> loadGraph(Graph.demo(), "встроенный пример"));
        p.add(demo);

        saveBtn = new JButton("Сохранить результат");
        saveBtn.addActionListener(e -> saveResult());
        p.add(saveBtn);

        JButton clear = new JButton("Очистить");
        clear.addActionListener(e -> clearGraph());
        p.add(clear);

        p.add(Box.createHorizontalStrut(24));

        JButton about = new JButton("О разработчиках");
        about.addActionListener(e -> showAbout());
        p.add(about);
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
        bottom.add(new JScrollPane(log), BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        south.add(statusBar, BorderLayout.NORTH);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER));

        backBtn = new JButton("Шаг назад");
        backBtn.addActionListener(e -> doStepBack());
        controls.add(backBtn);

        stepBtn = new JButton("Шаг");
        stepBtn.addActionListener(e -> doStep());
        controls.add(stepBtn);

        autoBtn = new JButton("Автопрогон");
        autoBtn.addActionListener(e -> toggleAuto());
        controls.add(autoBtn);

        controls.add(new JLabel("Скорость:"));
        speed = new JSlider(100, 2000, 700);
        speed.setToolTipText("интервал автопрогона, мс");
        speed.addChangeListener(e -> {
            if (autoTimer != null) autoTimer.setDelay(speed.getValue());
        });
        controls.add(speed);

        resetBtn = new JButton("Сброс");
        resetBtn.addActionListener(e -> doReset());
        controls.add(resetBtn);

        south.add(controls, BorderLayout.SOUTH);
        bottom.add(south, BorderLayout.SOUTH);
        return bottom;
    }

    private void openFile() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            Graph g = GraphIO.load(chooser.getSelectedFile().toPath());
            loadGraph(g, chooser.getSelectedFile().getName());
            // файл, сохранённый программой как результат, показываем сразу
            // в завершённом виде - как будто алгоритм уже прогнали
            if (GraphIO.isResultFile(chooser.getSelectedFile().toPath()))
                showFinished();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Не удалось загрузить граф:\n" + ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    /* Прокрутить алгоритм до конца и показать итог (для файлов результата). */
    private void showFinished() {
        canvas.setEditable(false);
        String msg = "";
        while (!stepper.isFinished()) msg = stepper.stepForward();
        log.append("Файл распознан как сохранённый результат - показано"
                + " завершённое выполнение алгоритма.\n" + msg + "\n"
                + "\"Шаг назад\" - разобрать выполнение по шагам, \"Сброс\" -"
                + " начать заново.\n");
        scrollLog();
        status(msg);
        refreshState();
        canvas.repaint();
        updateButtons();
    }

    private void clearGraph() {
        if (canvas.graph() != null && canvas.graph().vertexCount() > 0) {
            int ans = JOptionPane.showConfirmDialog(this,
                    "Удалить весь граф?", "Очистить", JOptionPane.YES_NO_OPTION);
            if (ans != JOptionPane.YES_OPTION) return;
        }
        loadGraph(new Graph(), "пустой холст");
    }

    private void loadGraph(Graph g, String sourceName) {
        stopAuto();
        canvas.setGraph(g);
        stepper = new TopoStepper(g);
        canvas.setStepper(stepper);
        canvas.setEditable(true);
        refreshState();
        updateButtons();
        log.setText("Граф (" + sourceName + "): " + g.vertexCount() + " вершин, "
                + g.edgeCount() + " рёбер.\n"
                + "Мышь: клик - вершина; клики по двум вершинам - ребро от первой"
                + " ко второй; перетаскивание - перенос; правый клик - удаление.\n"
                + "\"Шаг\" или \"Автопрогон\" - выполнение алгоритма.\n");
        status("Готово. Редактируйте граф или запускайте алгоритм.");
    }

    /* Правка графа мышью: пересоздать алгоритм под новую структуру. */
    @Override
    public void graphEdited(String whatHappened) {
        stopAuto();
        stepper = new TopoStepper(canvas.graph());
        canvas.setStepper(stepper);
        refreshState();
        updateButtons();
        log.append(whatHappened + "\n");
        scrollLog();
        status(whatHappened);
    }

    private void doStep() {
        if (stepper == null || stepper.isFinished()) return;
        canvas.setEditable(false); // правки - только после "Сброса"
        String msg = stepper.stepForward();
        log.append(msg + "\n");
        scrollLog();
        status(msg);
        refreshState();
        canvas.repaint();
        updateButtons();
        if (stepper.isFinished()) stopAuto();
    }

    private void doStepBack() {
        if (stepper == null || !stepper.isStarted()) return;
        stopAuto();
        String last = stepper.stepBack();
        log.append("Шаг назад.\n");
        scrollLog();
        status(last != null ? last : "Начальное состояние.");
        if (!stepper.isStarted()) canvas.setEditable(true);
        refreshState();
        canvas.repaint();
        updateButtons();
    }

    private void doReset() {
        if (stepper == null) return;
        stopAuto();
        stepper.reset();
        canvas.setEditable(true);
        refreshState();
        canvas.repaint();
        updateButtons();
        log.append("Сброс: алгоритм возвращён к началу, редактирование разрешено.\n");
        scrollLog();
        status("Сброшено.");
    }

    private void toggleAuto() {
        if (autoTimer != null && autoTimer.isRunning()) {
            stopAuto();
            return;
        }
        if (stepper == null || stepper.isFinished()) return;
        autoTimer = new Timer(speed.getValue(), e -> {
            if (stepper.isFinished()) stopAuto();
            else doStep();
        });
        autoTimer.start();
        autoBtn.setText("Пауза");
        status("Автопрогон: шаг каждые " + speed.getValue() + " мс.");
    }

    private void stopAuto() {
        if (autoTimer != null) autoTimer.stop();
        if (autoBtn != null) autoBtn.setText("Автопрогон");
    }

    private void saveResult() {
        Graph g = canvas.graph();
        if (g == null || g.vertexCount() == 0) {
            JOptionPane.showMessageDialog(this, "Сначала задайте граф.",
                    "Нет данных", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("результат-топосорт.txt"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            GraphIO.saveResult(chooser.getSelectedFile().toPath(), g, TopoSort.run(g));
            status("Результат сохранён: " + chooser.getSelectedFile().getName());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Не удалось сохранить файл:\n" + ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showAbout() {
        String text = """
                Визуализатор топологической сортировки (алгоритм Кана)
                Учебная практика, СПбГЭТУ "ЛЭТИ", 2026
                
                Бригада 1:
                  Беспалов Александр Александрович, 4383 - модель и алгоритм
                  Мазеев Владислав Антонович, 4381 - визуализация
                  Серженко Дмитрий Кириллович, 4383 - управление и файлы
                """;
        JOptionPane.showMessageDialog(this, text, "О разработчиках",
                JOptionPane.INFORMATION_MESSAGE, new ImageIcon(aboutImage()));
    }

    /* Картинка для диалога - нарисованный смайлик вместо фото. */
    private static BufferedImage aboutImage() {
        BufferedImage img = new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0xFFD54F));
        g.fillOval(4, 4, 88, 88);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(3f));
        g.drawOval(4, 4, 88, 88);
        g.fillOval(28, 32, 10, 10);
        g.fillOval(58, 32, 10, 10);
        g.drawArc(28, 44, 40, 28, 200, 140);
        g.dispose();
        return img;
    }

    private void refreshState() {
        stateModel.clear();
        if (stepper == null) return;
        stateModel.addElement("Очередь (in=0):");
        if (stepper.readyList().isEmpty()) stateModel.addElement("  (пусто)");
        for (int id : stepper.readyList()) stateModel.addElement("  " + id);
        stateModel.addElement("");
        stateModel.addElement("Порядок:");
        if (stepper.orderList().isEmpty()) stateModel.addElement("  (пусто)");
        for (int i = 0; i < stepper.orderList().size(); i++)
            stateModel.addElement("  " + (i + 1) + ") " + stepper.orderList().get(i));
        if (!stepper.stuckSet().isEmpty()) {
            stateModel.addElement("");
            stateModel.addElement("В цикле:");
            for (int id : stepper.stuckSet()) stateModel.addElement("  " + id);
        }
    }

    private void updateButtons() {
        boolean hasGraph = canvas.graph() != null && canvas.graph().vertexCount() > 0;
        stepBtn.setEnabled(hasGraph && stepper != null && !stepper.isFinished());
        backBtn.setEnabled(stepper != null && stepper.isStarted());
        autoBtn.setEnabled(hasGraph && stepper != null && !stepper.isFinished());
        resetBtn.setEnabled(stepper != null && stepper.isStarted());
        saveBtn.setEnabled(hasGraph);
    }

    private void scrollLog() {
        log.setCaretPosition(log.getDocument().getLength());
    }

    private void status(String s) {
        statusBar.setText(s.split("\n")[0]);
    }
}
