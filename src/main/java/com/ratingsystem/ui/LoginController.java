package com.ratingsystem.ui;

import com.ratingsystem.database.DatabaseManager;
import com.ratingsystem.database.UserService;
import com.ratingsystem.models.User;
import com.ratingsystem.utils.PasswordUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;

/**
 * Контроллер окна входа в систему
 */
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);
    private UserService userService = new UserService();

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Button registerButton;

    @FXML
    private Label errorLabel;

    private User currentUser;

    @FXML
    private void initialize() {
        errorLabel.setText("");
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Пожалуйста, заполните все поля");
            return;
        }

        try {
            currentUser = userService.authenticate(username, password);

            if (currentUser != null) {
                logger.info("User logged in: {} with role {}", username, currentUser.getRole());
                openMainWindow();
            } else {
                errorLabel.setText("Неверное имя пользователя или пароль");
            }
        } catch (Exception e) {
            logger.error("Login error", e);
            errorLabel.setText("Ошибка при входе в систему");
        }
    }

    @FXML
    private void handleRegister() {
        // Открыть окно регистрации
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/RegisterWindow.fxml"));
            Parent root = loader.load();
            
            Stage registerStage = new Stage();
            registerStage.setTitle("Регистрация");
            registerStage.setScene(new Scene(root, 500, 400));
            registerStage.show();
        } catch (Exception e) {
            logger.error("Error opening register window", e);
            errorLabel.setText("Ошибка при открытии окна регистрации");
        }
    }

    /**
     * Открыть главное окно приложения
     */
    private void openMainWindow() {
        try {
            logger.info("Loading MainWindow.fxml");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainWindow.fxml"));
            Parent root = loader.load();
            logger.info("MainWindow.fxml loaded successfully");
            
            logger.info("Getting main controller");
            MainController mainController = loader.getController();
            logger.info("Setting current user: {}", currentUser.getUsername());
            mainController.setCurrentUser(currentUser);

            Stage mainStage = new Stage();
            mainStage.setTitle("Rating System - " + currentUser.getUsername());
            mainStage.setScene(new Scene(root, 1000, 700));
            mainStage.show();
            logger.info("Main window displayed successfully");

            // Закрыть окно входа
            Stage loginStage = (Stage) loginButton.getScene().getWindow();
            loginStage.close();
        } catch (Exception e) {
            logger.error("Error opening main window", e);
            e.printStackTrace();
            errorLabel.setText("Ошибка при открытии главного окна: " + e.getClass().getSimpleName());
        }
    }
}
