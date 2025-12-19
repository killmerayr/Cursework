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

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

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
            
            // Извлекаем самое глубокое сообщение об ошибке
            Throwable cause = e;
            StringBuilder technicalInfo = new StringBuilder();
            while (cause != null) {
                technicalInfo.append(cause.getClass().getSimpleName())
                             .append(": ")
                             .append(cause.getMessage())
                             .append("\n");
                cause = cause.getCause();
            }
            
            showErrorAndExit("Ошибка инициализации", 
                "Не удалось подключиться к базе данных.\n\n" +
                "Возможные причины:\n" +
                "1. База данных не запущена (выполните 'docker-compose up -d')\n" +
                "2. Неверный пароль (проверьте DB_PASSWORD или application.properties)\n" +
                "3. Порт 54331 занят другим приложением\n\n" +
                "Техническая информация:\n" + technicalInfo.toString());
        }
    }

    private void showErrorAndExit(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait().ifPresent(rs -> System.exit(1));
        System.exit(1);
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
