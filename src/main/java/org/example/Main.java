package org.example;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main application class.
 * Responsible for environment setup, error handling, and launching the GUI.
 */
public class Main {
    // Logger setup (Java Util Logging)
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        // 1. Global exception handler setup
        // Ensures any unhandled exception (e.g., RuntimeException)
        // is logged, and the user sees a clear message.
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            LOGGER.log(Level.SEVERE, "Unexpected error in thread " + thread.getName(), throwable);
            JOptionPane.showMessageDialog(null,
                    "A critical error occurred: " + throwable.getMessage() + "\nSee logs for details.",
                    "Critical Error",
                    JOptionPane.ERROR_MESSAGE);
        });

        // 2. Launch GUI in Event Dispatch Thread (EDT)
        // Swing is not thread-safe, so launch must be wrapped in invokeLater.
        SwingUtilities.invokeLater(() -> {
            try {
                // Set system Look and Feel for better aesthetics
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

                // DB initialization happens inside getInstance
                StudentManagerImpl.getInstance();

                LOGGER.info("Application starting...");

                // LAUNCH ACTUAL WINDOW
                MainFrame mainFrame = new MainFrame();
                mainFrame.setVisible(true);

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error launching GUI", e);
                System.exit(1);
            }
        });
    }
}