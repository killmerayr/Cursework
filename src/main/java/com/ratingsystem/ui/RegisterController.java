package com.ratingsystem.ui;

import com.ratingsystem.database.DatabaseManager;
import com.ratingsystem.models.User;
import com.ratingsystem.utils.PasswordUtils;
import com.ratingsystem.utils.ValidationUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;

/**
 * Контроллер окна регистрации
 */
public class RegisterController {

    private static final Logger logger = LoggerFactory.getLogger(RegisterController.class);
    private com.ratingsystem.database.UserService userService = new com.ratingsystem.database.UserService();

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private ComboBox<String> roleComboBox;

    @FXML
    private Label messageLabel;

    @FXML
    private void initialize() {
        // По умолчанию все новые пользователи - Гости. 
        // Роль Модератора может назначить только Администратор через БД или панель управления.
        roleComboBox.setItems(FXCollections.observableArrayList("GUEST"));
        roleComboBox.setValue("GUEST");
        roleComboBox.setDisable(true); // Блокируем выбор, чтобы нельзя было подменить через UI
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String role = roleComboBox.getValue();

        // Валидация
        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Пожалуйста, заполните все поля");
            messageLabel.setStyle("-fx-text-fill: #d32f2f;");
            return;
        }

        if (!ValidationUtils.isValidUsername(username)) {
            messageLabel.setText("Имя пользователя должно содержать 3-20 символов (буквы, цифры, _)");
            messageLabel.setStyle("-fx-text-fill: #d32f2f;");
            return;
        }

        if (!password.equals(confirmPassword)) {
            messageLabel.setText("Пароли не совпадают");
            messageLabel.setStyle("-fx-text-fill: #d32f2f;");
            return;
        }

        if (!PasswordUtils.isStrongPassword(password)) {
            messageLabel.setText("Пароль должен содержать минимум 8 символов, цифру и заглавную букву");
            messageLabel.setStyle("-fx-text-fill: #d32f2f;");
            return;
        }

        // Принудительно устанавливаем роль GUEST, игнорируя что пришло из UI (на всякий случай)
        String finalRole = "GUEST";

        // Проверить, существует ли пользователь
        try {
            if (userService.getUserByUsername(username) != null) {
                messageLabel.setText("Пользователь с таким именем уже существует");
                messageLabel.setStyle("-fx-text-fill: #d32f2f;");
                return;
            }

            // Создать пользователя с ролью GUEST
            userService.createUser(username, password, User.UserRole.GUEST);
            
            messageLabel.setText("Регистрация успешна! Теперь вы можете войти в систему.");
            messageLabel.setStyle("-fx-text-fill: #4caf50;");
            logger.info("New user registered: {} with role GUEST", username);

            // Закрыть окно через 2 секунды
            javafx.application.Platform.runLater(() -> {
                try {
                    Thread.sleep(1500);
                    Stage stage = (Stage) usernameField.getScene().getWindow();
                    stage.close();
                } catch (InterruptedException e) {
                    logger.error("Error closing window", e);
                }
            });
        } catch (Exception e) {
            logger.error("Registration error", e);
            messageLabel.setText("Ошибка при регистрации");
            messageLabel.setStyle("-fx-text-fill: #d32f2f;");
        }
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.close();
    }
}
