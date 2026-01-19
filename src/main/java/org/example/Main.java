package org.example;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Главный класс приложения.
 * Отвечает за инициализацию окружения, настройку обработки ошибок и запуск GUI.
 */
public class Main {
    // Настройка логгера (Java Util Logging)
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        // 1. Настройка глобального перехватчика исключений
        // Это гарантирует, что любое необработанное исключение (например, RuntimeException)
        // будет залогировано, а пользователь увидит понятное сообщение.
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            LOGGER.log(Level.SEVERE, "Непредвиденная ошибка в потоке " + thread.getName(), throwable);
            JOptionPane.showMessageDialog(null,
                    "Произошла критическая ошибка: " + throwable.getMessage() + "\nСм. логи для деталей.",
                    "Критическая ошибка",
                    JOptionPane.ERROR_MESSAGE);
        });

        // 2. Запуск GUI в потоке диспетчеризации событий (EDT)
        // Swing не является потокобезопасным, поэтому запуск должен быть обернут в invokeLater.
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

                // Инициализация БД происходит внутри getInstance
                StudentManagerImpl.getInstance();

                LOGGER.info("Приложение запускается...");

                // ЗАПУСК НАСТОЯЩЕГО ОКНА
                MainFrame mainFrame = new MainFrame();
                mainFrame.setVisible(true);

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Ошибка при запуске GUI", e);
                System.exit(1);
            }
        });
    }
}