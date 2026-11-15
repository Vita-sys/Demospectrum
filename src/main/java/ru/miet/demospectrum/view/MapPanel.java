package ru.miet.demospectrum.view;

import ru.miet.demospectrum.model.City;
import ru.miet.demospectrum.service.ErrMsgLog;
import ru.miet.demospectrum.service.MapDataService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class MapPanel extends JPanel {

    private JTable cityTable;
    private DefaultTableModel tableModel;
    private JLabel infoLabel;
    private MapDataService dataService = new MapDataService();

    private int currentYear = 1990;
    private double currentThreshold = 10.0;

    private JSlider yearSlider;
    private JSlider thresholdSlider;
    private JLabel yearLabel;
    private JLabel thresholdLabel;

    // Вспомогательный класс-мост для вызова Java-методов из JavaScript карты
    public class MapBridge {
        public void openChart(String cityName) {
            SwingUtilities.invokeLater(() -> showChartForCity(cityName));
        }
    }

    private final MapBridge mapBridge = new MapBridge();

    public MapPanel() {
        setLayout(new BorderLayout());
        initUI();
    }

    private void initUI() {
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Фильтры"));

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

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton openMapButton = new JButton("Показать карту");
        openMapButton.addActionListener(e -> openMapInBrowser());
        buttonPanel.add(openMapButton);
        controlPanel.add(buttonPanel);

        add(controlPanel, BorderLayout.NORTH);

        String[] columns = {"Город", "Широта", "Долгота", "Изменение (%)"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        cityTable = new JTable(tableModel);
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

        infoLabel = new JLabel("Загрузка данных...");
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(infoLabel, BorderLayout.SOUTH);

        refreshTable();
    }

    private void refreshTable() {
        ErrMsgLog logger = new ErrMsgLog();
        try {
            List<City> cities = dataService.loadCitiesFiltered(currentYear, currentThreshold);
            tableModel.setRowCount(0);

            for (City city : cities) {
                double change = city.getChangePercent();
                tableModel.addRow(new Object[]{
                        city.getName(),
                        city.getLatitude(),
                        city.getLongitude(),
                        String.format("%.2f", change)
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
            int port = ru.miet.demospectrum.service.TileProxyServer.start();
            List<City> cities = dataService.loadCitiesFiltered(currentYear, currentThreshold);
            String html = generateHtml(cities, port);

            JFrame mapFrame = new JFrame("Карта: " + currentYear + " год (порог " + currentThreshold + "%)");
            mapFrame.setSize(1050, 720);
            mapFrame.setLocationRelativeTo(this);

            javafx.embed.swing.JFXPanel fxPanel = new javafx.embed.swing.JFXPanel();
            mapFrame.add(fxPanel);

            javafx.application.Platform.runLater(() -> {
                javafx.scene.web.WebView webView = new javafx.scene.web.WebView();
                javafx.scene.web.WebEngine webEngine = webView.getEngine();

                webView.widthProperty().addListener((obs, oldVal, newVal) ->
                        webEngine.executeScript("if (window.map) { map.invalidateSize(true); }"));
                webView.heightProperty().addListener((obs, oldVal, newVal) ->
                        webEngine.executeScript("if (window.map) { map.invalidateSize(true); }"));

                webEngine.loadContent(html);

                webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                    if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                        netscape.javascript.JSObject window = (netscape.javascript.JSObject) webEngine.executeScript("window");
                        window.setMember("javaApp", mapBridge);
                        webEngine.executeScript("setTimeout(function() { map.invalidateSize(true); map.fitBounds([[41.0, 27.0], [72.0, 175.0]]); }, 300);");
                    }
                });

                fxPanel.setScene(new javafx.scene.Scene(webView));
            });

            mapFrame.setVisible(true);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Ошибка карты: " + e.getMessage());
        }
    }

    private String generateHtml(List<City> cities, int port) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='utf-8'/>");
        html.append("<title>Демоспектр — Карта</title>");
        html.append("<link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.css'/>");
        html.append("<script>window.L_DISABLE_3D = true;</script>");
        html.append("<script src='https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.js'></script>");

        html.append("<style>");
        html.append("  * { box-sizing: border-box; }");
        html.append("  html, body, #map { margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden; background: #e5e3df; font-family: sans-serif; }");
        html.append("  .chart-btn { margin-top: 6px; padding: 4px 10px; background: #2b7de9; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 12px; }");
        html.append("  .chart-btn:hover { background: #1b66c9; }");
        html.append("  .legend { background: rgba(255, 255, 255, 0.92); padding: 10px 12px; border-radius: 6px; box-shadow: 0 1px 5px rgba(0,0,0,0.3); line-height: 20px; font-size: 12px; color: #333; }");
        html.append("  .legend i { width: 12px; height: 12px; float: left; margin-right: 8px; margin-top: 4px; border-radius: 50%; border: 1px solid #fff; box-shadow: 0 0 2px rgba(0,0,0,0.4); }");
        html.append("</style>");

        html.append("</head><body><div id='map'></div><script>");

        html.append("var map = L.map('map', {");
        html.append("  zoomAnimation: false,");
        html.append("  fadeAnimation: false,");
        html.append("  markerZoomAnimation: false");
        html.append("});");

        // Сразу показываем всю Россию (от Калининграда до Камчатки)
        html.append("map.fitBounds([[41.0, 27.0], [72.0, 175.0]]);");

        html.append("L.tileLayer('http://127.0.0.1:").append(port).append("/tiles/{z}/{x}/{y}.png', {");
        html.append("  maxZoom: 18,");
        html.append("  attribution: '&copy; OpenStreetMap contributors'");
        html.append("}).addTo(map);");

        double midLevel = currentThreshold * 1.5;
        double highLevel = currentThreshold * 2.5;

        if (cities != null) {
            for (City city : cities) {
                double lat = city.getLatitude();
                double lon = city.getLongitude();
                double change = city.getChangePercent();
                double absChange = Math.abs(change);

                String fillColor;
                int radius;
                if (absChange >= highLevel) {
                    fillColor = "#e63946"; // Красный — очень сильная аномалия
                    radius = 8;
                } else if (absChange >= midLevel) {
                    fillColor = "#f4a261"; // Желто-оранжевый — средняя аномалия
                    radius = 7;
                } else {
                    fillColor = "#2a9d8f"; // Зеленый — незначительная аномалия
                    radius = 6;
                }

                String safeName = city.getName().replace("'", "\\'").replace("\"", "&quot;");
                String changeStr = String.format(java.util.Locale.US, "%.2f", change);

                String popupHtml = "<b>" + safeName + "</b><br>"
                        + "Год: " + currentYear + "<br>"
                        + "Аномалия: <b style=\"color:" + fillColor + ";\">" + changeStr + "%</b><br>"
                        + "<button class=\"chart-btn\" onclick=\"javaApp.openChart(\\'" + safeName + "\\')\">Открыть график</button>";

                html.append(String.format(java.util.Locale.US,
                        "L.circleMarker([%.6f, %.6f], { radius: %d, fillColor: '%s', color: '#ffffff', weight: 1.5, opacity: 1, fillOpacity: 0.9 })"
                                + ".addTo(map).bindPopup('%s');",
                        lat, lon, radius, fillColor, popupHtml));
            }
        }

        // Добавляем легенду
        html.append("var legend = L.control({position: 'bottomleft'});");
        html.append("legend.onAdd = function(map) {");
        html.append("  var div = L.DomUtil.create('div', 'legend');");
        html.append(String.format(java.util.Locale.US,
                "  div.innerHTML = '<b>Уровень аномалии</b><br>' +"
                        + "'<i style=\"background:#2a9d8f\"></i> Мало (%.1f%% – %.1f%%)<br>' +"
                        + "'<i style=\"background:#f4a261\"></i> Средне (%.1f%% – %.1f%%)<br>' +"
                        + "'<i style=\"background:#e63946\"></i> Много (> %.1f%%)';",
                currentThreshold, midLevel, midLevel, highLevel, highLevel));
        html.append("  return div;");
        html.append("};");
        html.append("legend.addTo(map);");

        html.append("</script></body></html>");
        return html.toString();
    }

    private void showChartForCity(String cityName) {
        try {
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

            ru.miet.demospectrum.dao.PopulationDAO pDao = new ru.miet.demospectrum.dao.PopulationDAO();
            java.util.List<ru.miet.demospectrum.model.PopulationRecord> records = pDao.getByCityId(cityId);

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