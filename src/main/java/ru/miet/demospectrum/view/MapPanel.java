package ru.miet.demospectrum.view;

import ru.miet.demospectrum.model.City;
import ru.miet.demospectrum.service.ErrMsgLog;
import ru.miet.demospectrum.service.MapDataService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

public class MapPanel extends JPanel {

    private JTable cityTable;
    private DefaultTableModel tableModel;
    private JLabel infoLabel;
    private MapDataService dataService = new MapDataService();

    // Текущие настройки фильтрации
    private int currentYear = 1990;        // Год
    private double currentThreshold = 10.0; // Порог аномалии (%)

    // Компоненты управления
    private JSlider yearSlider;
    private JSlider thresholdSlider;
    private JLabel yearLabel;
    private JLabel thresholdLabel;

    public MapPanel() {
        setLayout(new BorderLayout());
        initUI();
    }

    private void initUI() {
        // === 1. Панель управления (сверху) ===
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Фильтры"));

        // 1.1. Слайдер года
        JPanel yearPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        yearLabel = new JLabel("Год: " + currentYear);
        yearSlider = new JSlider(1900, 2025, currentYear);
        yearSlider.setMajorTickSpacing(25);
        yearSlider.setPaintTicks(true);
        yearSlider.setPreferredSize(new Dimension(400, 50));
        yearSlider.addChangeListener(e -> {
            currentYear = yearSlider.getValue();
            yearLabel.setText("Год: " + currentYear);
            refreshTable();
        });
        yearPanel.add(yearLabel);
        yearPanel.add(yearSlider);
        controlPanel.add(yearPanel);

        // 1.2. Слайдер порога аномалии
        JPanel thresholdPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        thresholdLabel = new JLabel("Порог аномалии: " + (int)currentThreshold + "%");
        thresholdSlider = new JSlider(5, 50, (int)currentThreshold);
        thresholdSlider.setMajorTickSpacing(5);
        thresholdSlider.setPaintTicks(true);
        thresholdSlider.setPreferredSize(new Dimension(400, 50));
        thresholdSlider.addChangeListener(e -> {
            currentThreshold = thresholdSlider.getValue();
            thresholdLabel.setText("Порог аномалии: " + (int)currentThreshold + "%");
            refreshTable();
        });
        thresholdPanel.add(thresholdLabel);
        thresholdPanel.add(thresholdSlider);
        controlPanel.add(thresholdPanel);

        // 1.3. Кнопка открытия карты
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton openMapButton = new JButton("Показать карту");
        openMapButton.addActionListener(e -> openMapInBrowser());
        buttonPanel.add(openMapButton);
        controlPanel.add(buttonPanel);

        add(controlPanel, BorderLayout.NORTH);

        // === 2. Таблица (в центре) ===
        String[] columns = {"Город", "Широта", "Долгота", "Изменение (%)"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        cityTable = new JTable(tableModel);
        // При двойном клике на город открываем график
        cityTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int row = cityTable.getSelectedRow();
                    if (row != -1) {
                        String cityName = (String) tableModel.getValueAt(row, 0);
                        showChartForCity(cityName);
                    }
                }
            }
        });
        add(new JScrollPane(cityTable), BorderLayout.CENTER);

        // === 3. Статус (снизу) ===
        infoLabel = new JLabel("Загрузка данных...");
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(infoLabel, BorderLayout.SOUTH);

        // Первоначальная загрузка
        refreshTable();
    }

    private void refreshTable() {
        ErrMsgLog logger = new ErrMsgLog();
        try {
            // Загружаем города с учетом года и порога
            List<City> cities = dataService.loadCitiesFiltered(currentYear, currentThreshold);

            tableModel.setRowCount(0); // Очищаем таблицу

            for (City city : cities) {
                // Получаем процент изменения для этого города в этом году
                double change = city.getChangePercent();

                tableModel.addRow(new Object[]{
                        city.getName(),
                        city.getLatitude(),
                        city.getLongitude(),
                        change
                });
            }

            infoLabel.setText("Год: " + currentYear + " | Порог: " + currentThreshold + "% | Найдено городов: " + cities.size());

        } catch (Exception e) {
            logger.addErrWithLog(e);
            infoLabel.setText("Ошибка загрузки: " + e.getMessage());
        }
    }

    private void openMapInBrowser() {
        try {
            List<City> cities = dataService.loadCitiesFiltered(currentYear, currentThreshold);
            String html = generateHtml(cities);

            JFrame mapFrame = new JFrame("Карта: " + currentYear + " год (порог " + currentThreshold + "%)");
            mapFrame.setSize(1000, 700);
            mapFrame.setLocationRelativeTo(this);

            javafx.embed.swing.JFXPanel fxPanel = new javafx.embed.swing.JFXPanel();
            mapFrame.add(fxPanel);

            javafx.application.Platform.runLater(() -> {
                javafx.scene.web.WebView webView = new javafx.scene.web.WebView();
                javafx.scene.web.WebEngine webEngine = webView.getEngine();

                // Шаг 1: Очищаем кэш WebView
                webEngine.getHistory().getEntries().clear();
                java.net.CookieHandler.setDefault(new java.net.CookieManager());

                // Загружаем контент
                webEngine.loadContent(html);

                // Шаг 3: Увеличиваем задержку до 1000 мс (по совету Джемини)
                webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                    if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                        webEngine.executeScript("setTimeout(function() { map.invalidateSize(); }, 1000);");
                    }
                });

                fxPanel.setScene(new javafx.scene.Scene(webView));
            });

            mapFrame.setVisible(true);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Ошибка карты: " + e.getMessage());
        }
    }

    private String generateHtml(List<City> cities) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='utf-8'/>");
        html.append("<title>Демоспектр — Карта</title>");
        html.append("<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>");
        html.append("<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>");
        html.append("<style> html, body, #map { width: 100%; height: 100%; margin: 0; padding: 0; background: #fff; } </style>");
        html.append("</head><body><div id='map'></div><script>");

        // 1. Включаем режим простых пиксельных координат
        html.append("var map = L.map('map', { crs: L.CRS.Simple, minZoom: -3, maxZoom: 3 });");

        // 2. Путь к картинке
        String imagePath = MapPanel.class.getResource("/world_map.png").toExternalForm();

        // 3. Задаем РЕАЛЬНЫЕ размеры карты (13080 x 5870) из ТЗ
        html.append("var bounds = [[0, 0], [5870, 13080]];");
        html.append("L.imageOverlay('").append(imagePath).append("', bounds).addTo(map);");
        html.append("map.fitBounds(bounds);");

        // 4. МАТЕМАТИЧЕСКИЙ ПЕРЕСЧЕТ С КОМПЕНСАЦИЕЙ РАСТЯЖЕНИЯ КАРТИНКИ
        if (cities != null) {
            for (City city : cities) {
                double lat = city.getLatitude();
                double lon = city.getLongitude();

                // 1. Пересчет долготы (X) с компенсацией ширины
                // Картинка растянута по ширине, поэтому используем коэффициент 1.0 (360 градусов = 13080 пикселей)
                double xPixel = ((lon + 180.0) / 360.0) * 13080.0;

                // 2. Пересчет широты (Y) с компенсацией высоты
                // Используем формулу Меркатора, но с поправкой на сжатие картинки по высоте
                double latRad = Math.toRadians(lat);
                double mercatorY = Math.log(Math.tan(Math.PI / 4.0 + latRad / 2.0));

                // Границы обрезки твоей картинки (снизу -60, сверху 85)
                double minY = Math.log(Math.tan(Math.PI / 4.0 + Math.toRadians(-60.0) / 2.0));
                double maxY = Math.log(Math.tan(Math.PI / 4.0 + Math.toRadians(85.0) / 2.0));

                // Пересчет Y с учетом того, что картинка сжата по высоте (5870 пикселей)
                double yPixel = ((mercatorY - minY) / (maxY - minY)) * 5870.0;

                // В Leaflet (CRS.Simple) ось Y перевернута
                yPixel = 5870.0 - yPixel;

                String xStr = String.format(java.util.Locale.US, "%.2f", xPixel);
                String yStr = String.format(java.util.Locale.US, "%.2f", yPixel);

                html.append("L.marker([").append(yStr).append(", ").append(xStr).append("])")
                        .append(".addTo(map)")
                        .append(".bindPopup('").append(city.getName().replace("'", "\\'")).append("');");
            }
        }

        // Лечим серые квадраты WebView
        html.append("setTimeout(function() { map.invalidateSize(); }, 600);");

        html.append("</script></body></html>");
        return html.toString();
    }

    private void showChartForCity(String cityName) {
        try {
            // 1. Ищем ID города
            int cityId = -1;
            java.util.List<City> cities = dataService.loadCitiesFiltered(currentYear, currentThreshold);
            for (City c : cities) {
                if (c.getName().equals(cityName)) {
                    cityId = c.getId();
                    break;
                }
            }
            if (cityId == -1) {
                JOptionPane.showMessageDialog(this, "Город не найден!");
                return;
            }

            // 2. Получаем данные о населении
            ru.miet.demospectrum.dao.PopulationDAO pDao = new ru.miet.demospectrum.dao.PopulationDAO();
            java.util.List<ru.miet.demospectrum.model.PopulationRecord> records = pDao.getByCityId(cityId);

            // 3. Создаем окно с графиком
            JFrame chartFrame = new JFrame("График: " + cityName);
            chartFrame.setSize(800, 600);
            chartFrame.setLocationRelativeTo(this);

            PopulationChartPanel chartPanel = new PopulationChartPanel();
            chartPanel.setData(cityName, records);

            chartFrame.add(chartPanel);
            chartFrame.setVisible(true);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ошибка при построении графика: " + ex.getMessage());
        }
    }

}