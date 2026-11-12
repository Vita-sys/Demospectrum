package ru.miet.demospectrum.view;

import ru.miet.demospectrum.model.PopulationRecord;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PopulationChartPanel extends JPanel {
    private List<PopulationRecord> records = new ArrayList<>();
    private String cityName = "Город";

    public void setData(String cityName, List<PopulationRecord> records) {
        this.cityName = cityName != null ? cityName : "Город";
        this.records = records != null ? records : new ArrayList<>();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        int padding = 50;
        int labelPadding = 30;

        // Заголовок графика
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.setColor(UIManager.getColor("Label.foreground"));
        g2.drawString("Динамика численности населения: " + cityName, padding, 30);

        if (records.isEmpty()) {
            g2.setFont(new Font("Arial", Font.PLAIN, 14));
            g2.setColor(Color.GRAY);
            g2.drawString("Нет данных для отображения графика", width / 2 - 110, height / 2);
            return;
        }

        // Находим минимальные и максимальные значения
        long minPop = records.stream().mapToLong(PopulationRecord::getPopulation).min().orElse(0);
        long maxPop = records.stream().mapToLong(PopulationRecord::getPopulation).max().orElse(100);
        
        maxPop = (long) (maxPop * 1.1);
        if (minPop > 0) minPop = (long) (minPop * 0.9);

        int minYear = records.stream().mapToInt(PopulationRecord::getYear).min().orElse(2000);
        int maxYear = records.stream().mapToInt(PopulationRecord::getYear).max().orElse(2026);
        if (minYear == maxYear) maxYear++;

        int graphWidth = width - 2 * padding - labelPadding;
        int graphHeight = height - 2 * padding - labelPadding;

        // Оси координат
        g2.setColor(UIManager.getColor("Component.borderColor"));
        g2.drawLine(padding + labelPadding, height - padding, width - padding, height - padding);
        g2.drawLine(padding + labelPadding, padding, padding + labelPadding, height - padding);

        // Линии сетки и подписи по оси Y
        int yDivisions = 5;
        for (int i = 0; i <= yDivisions; i++) {
            int yCoord = height - padding - i * graphHeight / yDivisions;
            long value = minPop + (maxPop - minPop) * i / yDivisions;
            
            g2.setColor(new Color(200, 200, 200, 100));
            g2.drawLine(padding + labelPadding, yCoord, width - padding, yCoord);

            g2.setColor(UIManager.getColor("Label.foreground"));
            g2.setFont(new Font("Arial", Font.PLAIN, 10));
            String label = formatPopulation(value);
            g2.drawString(label, padding, yCoord + 4);
        }

        // Координаты точек
        int[] xPoints = new int[records.size()];
        int[] yPoints = new int[records.size()];

        for (int i = 0; i < records.size(); i++) {
            PopulationRecord rec = records.get(i);
            xPoints[i] = padding + labelPadding + (int) ((double) (rec.getYear() - minYear) / (maxYear - minYear) * graphWidth);
            yPoints[i] = height - padding - (int) ((double) (rec.getPopulation() - minPop) / (maxPop - minPop == 0 ? 1 : (maxPop - minPop)) * graphHeight);
        }

        // Линия графика
        g2.setColor(new Color(51, 153, 255));
        g2.setStroke(new BasicStroke(2.5f));
        for (int i = 0; i < records.size() - 1; i++) {
            g2.drawLine(xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1]);
        }

        // Точки и годы
        for (int i = 0; i < records.size(); i++) {
            g2.setColor(new Color(0, 102, 204));
            g2.fillOval(xPoints[i] - 4, yPoints[i] - 4, 8, 8);
            g2.setColor(Color.WHITE);
            g2.fillOval(xPoints[i] - 2, yPoints[i] - 2, 4, 4);

            if (records.size() <= 12 || i % (records.size() / 10 + 1) == 0) {
                g2.setColor(UIManager.getColor("Label.foreground"));
                g2.setFont(new Font("Arial", Font.PLAIN, 10));
                String yearStr = String.valueOf(records.get(i).getYear());
                g2.drawString(yearStr, xPoints[i] - 12, height - padding + 15);
            }
        }
    }

    private String formatPopulation(long pop) {
        if (pop >= 1_000_000) {
            return String.format("%.1f млн", pop / 1_000_000.0);
        } else if (pop >= 1_000) {
            return String.format("%d тыс", pop / 1_000);
        }
        return String.valueOf(pop);
    }
}