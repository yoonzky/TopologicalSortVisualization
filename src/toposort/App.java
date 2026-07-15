package toposort;

import toposort.ui.MainFrame;

import javax.swing.SwingUtilities;

/*
 * Визуализатор топологической сортировки.
 * Пошаговая визуализация с пояснениями, шаг вперёд
 * и назад, автопрогон с задаваемым интервалом, редактирование графа
 * мышью, загрузка из файла и сохранение результата, обработка ошибок.
 */
public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
