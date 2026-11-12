package ru.miet.demospectrum.view;
import ru.miet.demospectrum.service.ErrMsgLog;
import ru.miet.demospectrum.config.Preloader;
import ru.miet.demospectrum.service.FileExportService;
import ru.miet.demospectrum.service.FileImportService;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainWindow extends JFrame {

    private JLabel statusLabel;
    private JTextArea logArea;

    public MainWindow() {
        setTitle("Демоспектр — Визуализация демографии России");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // Меню
        setJMenuBar(createMenuBar());

        // Центральная область
        JPanel centerPanel = createCenterPanel();
        add(centerPanel, BorderLayout.CENTER);

        // Статус-панель
        statusLabel = new JLabel("Готов к работе");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        add(statusLabel, BorderLayout.SOUTH);

        // === Логирование старта (Лаба №4) ===
        log("=== Программа запущена ===");
        log("Пользователь: " + Preloader.get("user.name")
                + " (" + Preloader.get("user.group") + ")");
        log("Время: " + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }

    /**
     * Создаёт меню: Файл, Отладка (root), О программе.
     */
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // === Меню "Файл" ===
        JMenu fileMenu = new JMenu("Файл");

        JMenuItem exportItem = new JMenuItem("Экспорт данных...");
        exportItem.addActionListener(e -> onExportData());

        JMenuItem importItem = new JMenuItem("Импорт данных...");
        importItem.addActionListener(e -> onImportData());

        JMenuItem exitItem = new JMenuItem("Выход");
        exitItem.addActionListener(e -> {
            log("=== Программа завершена ===");
            System.exit(0);
        });

        fileMenu.add(exportItem);
        fileMenu.add(importItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);

        // === Меню "Отладка" (только для root) ===
        String userGroup = Preloader.get("user.group");
        if ("root".equalsIgnoreCase(userGroup)) {
            JMenu debugMenu = new JMenu("Отладка");

            JMenuItem showConfigItem = new JMenuItem("Показать конфигурацию");
            showConfigItem.addActionListener(e -> showConfigDialog());

            JMenuItem clearLogItem = new JMenuItem("Очистить лог");
            clearLogItem.addActionListener(e -> clearLog());

            debugMenu.add(showConfigItem);
            debugMenu.add(clearLogItem);

            menuBar.add(debugMenu);
        }

        // === Меню "О программе" ===
        JMenu helpMenu = new JMenu("О программе");
        JMenuItem aboutItem = new JMenuItem("О разработчиках");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);

        menuBar.add(helpMenu);

        // === Меню "Вид" (Темы) ===
        JMenu viewMenu = new JMenu("Вид");

        JMenuItem lightThemeItem = new JMenuItem("Светлая тема");
        lightThemeItem.addActionListener(e -> {
            try {
                UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
                SwingUtilities.updateComponentTreeUI(this);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Ошибка темы: " + ex.getMessage());
            }
        });

        JMenuItem darkThemeItem = new JMenuItem("Тёмная тема");
        darkThemeItem.addActionListener(e -> {
            try {
                UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());
                SwingUtilities.updateComponentTreeUI(this);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Ошибка темы: " + ex.getMessage());
            }
        });

        viewMenu.add(lightThemeItem);
        viewMenu.add(darkThemeItem);
        menuBar.add(viewMenu);

        return menuBar;
    }

    /**
     * Центральная область: карта (сверху) + лог (снизу).
     */
    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        MapPanel mapPanel = new MapPanel();

        logArea = new JTextArea(8, 80);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Лог операций"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                mapPanel, logScroll);
        splitPane.setResizeWeight(0.75);
        splitPane.setDividerLocation(500);

        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Обработчик "Экспорт данных".
     */
    private void onExportData() {
        try {
            ErrMsgLog logger = new ErrMsgLog();
            logger.action("Экспорт", "Старт экспорта в export.csv");

            FileExportService service = new FileExportService();
            service.exportToCsv("export.csv");

            logger.action("Экспорт", "Успешно завершён: export.csv");
            log(" Экспорт завершён: export.csv");

            JOptionPane.showMessageDialog(this,
                    "Данные экспортированы в export.csv",
                    "Экспорт",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            new ErrMsgLog().addErrWithLog(e);
            log(" Ошибка экспорта: " + e.getMessage());
        }
    }

    /**
     * Обработчик "Импорт данных".
     */
    private void onImportData() {
        try {
            ErrMsgLog logger = new ErrMsgLog();
            logger.action("Импорт", "Открытие диалога выбора файла");

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Выберите CSV для импорта");
            int result = chooser.showOpenDialog(this);

            if (result == JFileChooser.APPROVE_OPTION) {
                String path = chooser.getSelectedFile().getAbsolutePath();
                logger.action("Импорт", "Выбран файл: " + path);

                FileImportService service = new FileImportService();
                int imported = service.importFromCsv(path);

                logger.action("Импорт", "Импортировано записей: " + imported);
                log("✅ Импорт завершён: " + path + " (" + imported + " записей)");
                JOptionPane.showMessageDialog(this,
                        "Импортировано записей: " + imported,
                        "Импорт",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            log(" Ошибка импорта: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Ошибка: " + e.getMessage(),
                    "Ошибка импорта",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Показывает окно с текущей конфигурацией (Отладка).
     */
    private void showConfigDialog() {
        String configText = "Текущая конфигурация:\n\n"
                + "Пользователь: " + Preloader.get("user.name") + "\n"
                + "Группа: " + Preloader.get("user.group") + "\n"
                + "Отладка: " + Preloader.get("debug.mode") + "\n"
                + "Автотесты: " + Preloader.get("autotest.mode");

        JOptionPane.showMessageDialog(this, configText,
                "Конфигурация", JOptionPane.INFORMATION_MESSAGE);
        log("Показана конфигурация");
    }

    /**
     * Очищает лог-панель (Отладка).
     */
    private void clearLog() {
        logArea.setText("");
        log("Лог очищен");
    }

    /**
     * Окно "О разработчиках".
     */
    private void showAboutDialog() {
        String message = "«Статистика изменения плотности населения России "
                + "и векторов миграции населения — Демоспектр»\n\n"
                + "Разработчики:\n"
                + "• Виталина Лысковская — Backend/Data\n"
                + "• Павел Елюков — Парсинг городов\n\n"
                + "Группа: ПИН-22 и ПИН-27\n"
                + "Год: 2026";
        JOptionPane.showMessageDialog(this, message, "О разработчиках",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Добавить сообщение в лог и статус-панель.
     */
    public void log(String message) {
        logArea.append(message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
        statusLabel.setText(message);
    }

    public void setStatus(String text) {
        statusLabel.setText(text);
    }

    public static void main(String[] args) {
        Preloader.loadConfig("config.properties");

        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
}