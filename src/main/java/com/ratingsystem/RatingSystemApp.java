package com.ratingsystem;

import com.ratingsystem.database.DatabaseManager;
import com.ratingsystem.database.UserService;
import com.ratingsystem.ui.LoginController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Главный класс приложения - точка входа
 */
public class RatingSystemApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(RatingSystemApp.class);

    @Override
    public void start(Stage primaryStage) {
        try {
            // Инициализация БД
            DatabaseManager.getInstance().initialize();
            logger.info("Database initialized successfully");

            // Инициализация данных
            new UserService().initializeDefaultAdmin();

            // Загрузка окна входа
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/LoginWindow.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 600, 400);
            primaryStage.setTitle("Rating System - Вход в систему");
            primaryStage.setScene(scene);
            primaryStage.show();

            logger.info("Application started");
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            System.exit(1);
        }
    }

    @Override
    public void stop() throws Exception {
        DatabaseManager.getInstance().close();
        logger.info("Application stopped");
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
