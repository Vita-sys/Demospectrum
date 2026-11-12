package ru.miet.demospectrum;

import ru.miet.demospectrum.config.Preloader;
import ru.miet.demospectrum.view.LoginWindow;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {

        // Маскируем приложение под браузер Chrome для загрузки карт
        System.setProperty("http.agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 DemospectrumApp/1.0");

        // Включаем современный вид (FlatLaf)
        try {
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
        } catch (Exception ex) {
            System.err.println("Не удалось загрузить тему: " + ex.getMessage());
        }

        // Загружаем конфигурацию
        Preloader.loadConfig("config.properties");

        // Открываем окно авторизации
        SwingUtilities.invokeLater(() -> {
            LoginWindow loginWindow = new LoginWindow();
            loginWindow.setVisible(true);
        });
    }
}