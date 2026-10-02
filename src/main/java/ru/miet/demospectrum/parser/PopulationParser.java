package ru.miet.demospectrum.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.miet.demospectrum.model.PopulationRecord;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PopulationParser implements DataParser<PopulationRecord> {

    @Override
    public List<PopulationRecord> parse(String url, int cityId) throws IOException {
        return parsePopulation(url, cityId);
    }

    public List<PopulationRecord> parsePopulation(String url, int cityId) throws IOException {
        List<PopulationRecord> records = new ArrayList<>();

        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .timeout(15000)
                .maxBodySize(0)
                .get();

        Elements tables = doc.select("table.standard");

        for (Element table : tables) {
            Elements rows = table.select("tr");

            for (int r = 0; r < rows.size() - 1; r++) {
                Element yearRow = rows.get(r);
                Elements yearCells = yearRow.select("th, td");

                List<Integer> years = new ArrayList<>();
                for (Element cell : yearCells) {
                    String text = cell.text();
                    int bracket = text.indexOf('[');
                    if (bracket > 0) {
                        text = text.substring(0, bracket);
                    }
                    text = text.trim();
                    try {
                        int year = Integer.parseInt(text);
                        if (year >= 1800 && year <= 2100) {
                            years.add(year);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }

                if (years.size() >= 3) {
                    Element dataRow = rows.get(r + 1);
                    Elements dataCells = dataRow.select("th, td");

                    int count = Math.min(years.size(), dataCells.size());
                    for (int i = 0; i < count; i++) {
                        String text = dataCells.get(i).text().replaceAll("\\D", "");
                        if (text.isEmpty()) {
                            continue;
                        }
                        try {
                            long population = Long.parseLong(text);
                            if (population > 0) {
                                records.add(new PopulationRecord(cityId, years.get(i), population));
                            }
                        } catch (NumberFormatException ignored) {
                            //
                        }
                    }
                }
            }
        }
        return records;
    }

    public static void main(String[] args) throws IOException {
        PopulationParser parser = new PopulationParser();
        String url = "https://ru.wikipedia.org/wiki/Зеленоград";

        List<PopulationRecord> records = parser.parse(url, 1);

        System.out.println("Найдено записей: " + records.size());
        for (PopulationRecord r : records) {
            System.out.println("Год: " + r.getYear() + ", Население: " + r.getPopulation());
        }
    }
}