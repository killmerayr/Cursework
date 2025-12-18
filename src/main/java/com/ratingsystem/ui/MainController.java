package com.ratingsystem.ui;

import com.ratingsystem.database.DatabaseManager;
import com.ratingsystem.database.GroupService;
import com.ratingsystem.database.DisciplineService;
import com.ratingsystem.database.RatingService;
import com.ratingsystem.models.Discipline;
import com.ratingsystem.models.Group;
import com.ratingsystem.models.Rating;
import com.ratingsystem.models.User;
import com.ratingsystem.utils.PDFExporter;
import com.ratingsystem.utils.PDFImporter;
import com.ratingsystem.utils.ValidationUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.util.*;
import java.util.Map;

/**
 * Главный контроллер приложения
 */
public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML
    private Label userLabel;

    @FXML
    private MenuBar menuBar;

    @FXML
    private TabPane tabPane;

    @FXML
    private TableView<Group> groupsTable;

    @FXML
    private TableColumn<Group, String> groupCodeColumn;

    @FXML
    private TableColumn<Group, Integer> studentCountColumn;

    @FXML
    private TableColumn<Group, Integer> disciplineCountColumn;

    @FXML
    private ListView<Group> groupListView;

    @FXML
    private ListView<Discipline> disciplineListView;

    @FXML
    private TextArea contentArea;

    @FXML
    private TableView<Rating> ratingsTable;

    @FXML
    private TableColumn<Rating, Integer> studentNumColumn;

    @FXML
    private TableColumn<Rating, String> studentNameColumn;

    @FXML
    private TableColumn<Rating, String> ratingColumn;

    @FXML
    private Tab usersTab;

    @FXML
    private TableView<User> usersTable;

    @FXML
    private TableColumn<User, Integer> userIdColumn;

    @FXML
    private TableColumn<User, String> userNameColumn;

    @FXML
    private TableColumn<User, String> userRoleColumn;

    @FXML
    private Button createGroupBtn;

    @FXML
    private Button deleteGroupBtn;

    @FXML
    private Button deleteUserBtn;

    @FXML
    private Button changeRoleBtn;

    @FXML
    private Button addDisciplineBtn;

    @FXML
    private Button editDisciplineBtn;

    @FXML
    private Button deleteDisciplineBtn;

    @FXML
    private Button addRatingBtn;

    @FXML
    private Button editRatingBtn;

    @FXML
    private Button deleteRatingBtn;

    private User currentUser;
    private ObservableList<Group> groupsList = FXCollections.observableArrayList();
    private DatabaseManager db;
    private com.ratingsystem.database.GroupService groupService;
    private com.ratingsystem.database.DisciplineService disciplineService;
    private com.ratingsystem.database.RatingService ratingService;
    private com.ratingsystem.database.UserService userService;

    @FXML
    private void initialize() {
        try {
            logger.info("Initializing MainController");
            db = DatabaseManager.getInstance();
            groupService = new GroupService();
            disciplineService = new DisciplineService();
            ratingService = new RatingService();
            userService = new com.ratingsystem.database.UserService();
            
            logger.info("Setting up table columns");
            setupTableColumns();
            
            logger.info("Setting up ratings table columns");
            setupRatingsTableColumns();

            logger.info("Setting up users table columns");
            setupUsersTableColumns();
            
            logger.info("Setting up disciplines tab");
            setupDisciplinesTab();
            
            logger.info("Loading groups");
            loadGroups();

            logger.info("Loading users");
            loadUsers();
            
            logger.info("MainController initialized successfully");
        } catch (Exception e) {
            logger.error("Error during initialization", e);
            showError("Ошибка инициализации приложения: " + e.getMessage());
        }
    }

    /**
     * Настроить вкладку Дисциплины
     */
    private void setupDisciplinesTab() {
        // Загрузить группы в левый список
        groupListView.setItems(groupsList);
        
        // Когда выбирается группа - загружаем её дисциплины
        groupListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadDisciplinesForGroup(newVal.getId());
            }
        });
        
        // Когда выбирается дисциплина - загружаем рейтинги
        disciplineListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadRatingsForDiscipline(newVal.getId());
            }
        });
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        userLabel.setText(user.getUsername() + " (" + user.getRole().getDisplayName() + ")");
        setupMenuPermissions();
    }

    /**
     * Настроить доступ к меню в зависимости от роли пользователя
     */
    private void setupMenuPermissions() {
        boolean isAdmin = currentUser.getRole() == User.UserRole.ADMINISTRATOR;
        boolean isModerator = isAdmin || currentUser.getRole() == User.UserRole.MODERATOR;
        
        createGroupBtn.setDisable(!isModerator);
        if (deleteGroupBtn != null) deleteGroupBtn.setDisable(!isModerator);
        addDisciplineBtn.setDisable(!isModerator);
        if (editDisciplineBtn != null) editDisciplineBtn.setDisable(!isModerator);
        deleteDisciplineBtn.setDisable(!isModerator);
        addRatingBtn.setDisable(!isModerator);
        editRatingBtn.setDisable(!isModerator);
        deleteRatingBtn.setDisable(!isModerator);

        // Вкладка пользователей доступна только админам и модераторам
        if (usersTab != null) {
            if (!isModerator) {
                tabPane.getTabs().remove(usersTab);
            }
        }
        
        if (deleteUserBtn != null) deleteUserBtn.setDisable(!isAdmin); // Удалять юзеров может только админ
        if (changeRoleBtn != null) changeRoleBtn.setDisable(!isAdmin); // Менять роли может только админ
        
        logger.info("Permissions set for role: {}", currentUser.getRole());
    }

    /**
     * Настроить столбцы таблицы
     */
    private void setupTableColumns() {
        groupCodeColumn.setCellValueFactory(param -> 
                new javafx.beans.property.SimpleStringProperty(param.getValue().getGroupCode()));
        studentCountColumn.setCellValueFactory(param -> 
                new javafx.beans.property.SimpleIntegerProperty(param.getValue().getStudentCount()).asObject());
        disciplineCountColumn.setCellValueFactory(param -> 
                new javafx.beans.property.SimpleIntegerProperty(param.getValue().getDisciplineCount()).asObject());

        groupsTable.setItems(groupsList);
    }

    /**
     * Загрузить все группы из БД
     */
    private void loadGroups() {
        try {
            groupsList.clear();
            ResultSet rs = db.executeQuery("SELECT id, group_code, student_count, discipline_count FROM groups");
            while (rs.next()) {
                Group group = new Group(
                        rs.getInt("id"),
                        rs.getString("group_code"),
                        rs.getInt("student_count"),
                        rs.getInt("discipline_count")
                );
                groupsList.add(group);
            }
            logger.info("Loaded {} groups from database", groupsList.size());
        } catch (Exception e) {
            logger.error("Error loading groups", e);
            showError("Ошибка загрузки групп");
        }
    }

    /**
     * Применить стили к диалогу
     */
    private void styleDialog(Dialog<?> dialog) {
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        dialogPane.getStyleClass().add("dialog-pane");
        
        // Стилизация кнопок в диалоге
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.getStyleClass().add("button-primary");
        }
        
        Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
        if (cancelButton != null) {
            cancelButton.getStyleClass().add("button-secondary");
        }
    }

    @FXML
    private void handleCreateGroup() {
        if (!canModifyData()) {
            showError("У вас нет прав для этого действия");
            return;
        }

        Dialog<Group> dialog = new Dialog<>();
        dialog.setTitle("Добавить новую группу");
        styleDialog(dialog);

        // Создать форму
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField groupCodeField = new TextField();
        groupCodeField.setPromptText("Код группы");
        // Расширяем диапазоны, чтобы избежать авто-коррекции (clamping)
        Spinner<Integer> studentCountSpinner = new Spinner<>(-999999, 999999, 30);
        studentCountSpinner.setEditable(true);
        Spinner<Integer> disciplineCountSpinner = new Spinner<>(-999999, 999999, 3);
        disciplineCountSpinner.setEditable(true);

        grid.add(new Label("Код группы:"), 0, 0);
        grid.add(groupCodeField, 1, 0);
        grid.add(new Label("Количество студентов:"), 0, 1);
        grid.add(studentCountSpinner, 1, 1);
        grid.add(new Label("Количество дисциплин:"), 0, 2);
        grid.add(disciplineCountSpinner, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Предотвращаем закрытие окна при ошибке валидации
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            String groupCode = groupCodeField.getText().trim();
            String studentCountText = studentCountSpinner.getEditor().getText().trim();
            String disciplineCountText = disciplineCountSpinner.getEditor().getText().trim();

            try {
                int studentCount = Integer.parseInt(studentCountText);
                int disciplineCount = Integer.parseInt(disciplineCountText);

                if (!ValidationUtils.isValidGroupCode(groupCode)) {
                    showError("Неверный код группы");
                    event.consume();
                    return;
                }
                if (studentCount < 1 || studentCount > 300) {
                    showError("Количество студентов должно быть от 1 до 300");
                    event.consume();
                    return;
                }
                if (disciplineCount < 1 || disciplineCount > 8) {
                    showError("Количество дисциплин должно быть от 1 до 8");
                    event.consume();
                    return;
                }
            } catch (NumberFormatException e) {
                showError("Ошибка: Введите корректные числа для количества студентов и дисциплин");
                event.consume();
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                String groupCode = groupCodeField.getText().trim();
                int studentCount = Integer.parseInt(studentCountSpinner.getEditor().getText().trim());
                int disciplineCount = Integer.parseInt(disciplineCountSpinner.getEditor().getText().trim());
                return new Group(groupCode, studentCount, disciplineCount);
            }
            return null;
        });

        Optional<Group> result = dialog.showAndWait();
        result.ifPresent(group -> {
            try {
                groupService.createGroup(group);
                loadGroups();
                showInfo("Группа \"" + group.getGroupCode() + "\" успешно создана");
                logger.info("Group created: {}", group.getGroupCode());
            } catch (Exception e) {
                logger.error("Error creating group", e);
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("duplicate key")) {
                    showError("Ошибка: Группа с таким кодом уже существует.");
                } else {
                    showError("Ошибка при добавлении группы: " + errorMsg);
                }
            }
        });
    }

    @FXML
    private void handleDeleteGroup() {
        if (!canModifyData()) {
            showError("У вас нет прав для этого действия");
            return;
        }

        Group selectedGroup = groupsTable.getSelectionModel().getSelectedItem();
        if (selectedGroup == null) {
            showError("Пожалуйста, выберите группу для удаления");
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        styleDialog(confirmDialog);
        confirmDialog.setTitle("Подтверждение удаления");
        confirmDialog.setHeaderText("Удалить группу?");
        confirmDialog.setContentText("Вы уверены, что хотите удалить группу \"" + selectedGroup.getGroupCode() + "\"?\nВсе связанные данные также будут удалены.");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                groupService.deleteGroupById(selectedGroup.getId());
                
                loadGroups();
                groupListView.getSelectionModel().clearSelection();
                disciplineListView.getItems().clear();
                ratingsTable.getItems().clear();
                
                showInfo("Группа \"" + selectedGroup.getGroupCode() + "\" успешно удалена");
                logger.info("Group deleted: {}", selectedGroup.getGroupCode());
            } catch (Exception e) {
                logger.error("Error deleting group", e);
                showError("Ошибка при удалении группы: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleExportToPDF() {
        try {
            Group selectedGroup = groupsTable.getSelectionModel().getSelectedItem();
            if (selectedGroup == null) {
                showError("Пожалуйста, выберите группу для экспорта");
                return;
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Сохранить отчёт как PDF");
            fileChooser.setInitialFileName(selectedGroup.getGroupCode() + "_report.pdf");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
            );
            Stage stage = (Stage) menuBar.getScene().getWindow();
            java.io.File file = fileChooser.showSaveDialog(stage);
            
            if (file != null) {
                Map<String, Double> summary = ratingService.getSummaryByGroup(selectedGroup.getId());
                
                PDFExporter.exportSummaryToPDF(file.getAbsolutePath(), selectedGroup.getGroupCode(), summary);
                showInfo("PDF успешно экспортирован в:\n" + file.getAbsolutePath());
                logger.info("PDF exported for group: {}", selectedGroup.getGroupCode());
            }
        } catch (Exception e) {
            logger.error("Error exporting to PDF", e);
            showError("Ошибка при экспорте в PDF: " + e.getMessage());
        }
    }

    @FXML
    private void handleExportFullReport() {
        try {
            Group selectedGroup = groupsTable.getSelectionModel().getSelectedItem();
            if (selectedGroup == null) {
                showError("Пожалуйста, выберите группу для экспорта");
                return;
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Сохранить полный отчёт как PDF");
            fileChooser.setInitialFileName(selectedGroup.getGroupCode() + "_full_report.pdf");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            Stage stage = (Stage) menuBar.getScene().getWindow();
            java.io.File file = fileChooser.showSaveDialog(stage);
            
            if (file != null) {
                List<Discipline> disciplines = disciplineService.getDisciplinesByGroup(selectedGroup.getId());
                List<String[]> dataList = new ArrayList<>();
                
                for (Discipline d : disciplines) {
                    List<Rating> ratings = ratingService.getRatingsByDiscipline(d.getId());
                    for (Rating r : ratings) {
                        dataList.add(new String[]{
                            d.getDisciplineCode(),
                            String.valueOf(r.getStudentNumber()),
                            r.getStudentName(),
                            String.format("%.2f", r.getRating())
                        });
                    }
                }
                
                String[][] data = new String[dataList.size()][4];
                for (int i = 0; i < dataList.size(); i++) {
                    data[i] = dataList.get(i);
                }
                
                String[] headers = {"Дисциплина", "№", "ФИО Студента", "Рейтинг"};
                PDFExporter.exportFullReportToPDF(
                    file.getAbsolutePath(), 
                    "Полный отчёт по группе " + selectedGroup.getGroupCode(),
                    headers, 
                    data
                );
                
                showInfo("Полный отчёт успешно экспортирован");
            }
        } catch (Exception e) {
            logger.error("Error exporting full report", e);
            showError("Ошибка при экспорте: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        Stage stage = (Stage) menuBar.getScene().getWindow();
        stage.close();
    }

    /**
     * Настроить столбцы таблицы пользователей
     */
    private void setupUsersTableColumns() {
        userIdColumn.setCellValueFactory(param -> 
                new javafx.beans.property.SimpleIntegerProperty(param.getValue().getId()).asObject());
        userNameColumn.setCellValueFactory(param -> 
                new javafx.beans.property.SimpleStringProperty(param.getValue().getUsername()));
        userRoleColumn.setCellValueFactory(param -> 
                new javafx.beans.property.SimpleStringProperty(param.getValue().getRole().getDisplayName()));
    }

    /**
     * Загрузить список пользователей
     */
    @FXML
    private void loadUsers() {
        try {
            List<User> users = userService.getAllUsers();
            usersTable.setItems(FXCollections.observableArrayList(users));
            logger.info("Loaded {} users", users.size());
        } catch (Exception e) {
            logger.error("Error loading users", e);
        }
    }

    @FXML
    private void handleDeleteUser() {
        if (currentUser.getRole() != User.UserRole.ADMINISTRATOR) {
            showError("Только администратор может удалять пользователей");
            return;
        }

        User selectedUser = usersTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showError("Выберите пользователя для удаления");
            return;
        }

        if (selectedUser.getId() == currentUser.getId()) {
            showError("Вы не можете удалить самого себя");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        styleDialog(confirm);
        confirm.setTitle("Подтверждение");
        confirm.setContentText("Удалить пользователя \"" + selectedUser.getUsername() + "\"?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                userService.deleteUser(selectedUser.getId());
                loadUsers();
                showInfo("Пользователь удален");
            } catch (Exception e) {
                logger.error("Error deleting user", e);
                showError("Ошибка при удалении пользователя");
            }
        }
    }

    @FXML
    private void handleChangeRole() {
        if (currentUser.getRole() != User.UserRole.ADMINISTRATOR) {
            showError("Только администратор может изменять роли");
            return;
        }

        User selectedUser = usersTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showError("Выберите пользователя для изменения роли");
            return;
        }

        if (selectedUser.getUsername().equals("admin")) {
            showError("Нельзя изменить роль главного администратора");
            return;
        }

        ChoiceDialog<User.UserRole> dialog = new ChoiceDialog<>(selectedUser.getRole(), User.UserRole.values());
        styleDialog(dialog);
        dialog.setTitle("Изменение роли");
        dialog.setHeaderText("Выберите новую роль для пользователя " + selectedUser.getUsername());
        dialog.setContentText("Роль:");

        Optional<User.UserRole> result = dialog.showAndWait();
        result.ifPresent(newRole -> {
            try {
                userService.updateUserRole(selectedUser.getId(), newRole);
                loadUsers();
                showInfo("Роль пользователя " + selectedUser.getUsername() + " изменена на " + newRole.getDisplayName());
            } catch (Exception e) {
                logger.error("Error updating user role", e);
                showError("Ошибка при обновлении роли");
            }
        });
    }

    /**
     * Настроить столбцы таблицы рейтингов
     */
    private void setupRatingsTableColumns() {
        studentNumColumn.setCellValueFactory(param -> 
                new javafx.beans.property.SimpleIntegerProperty(param.getValue().getStudentNumber()).asObject());
        studentNameColumn.setCellValueFactory(param -> 
                new javafx.beans.property.SimpleStringProperty(param.getValue().getStudentName()));
        ratingColumn.setCellValueFactory(param -> 
                new javafx.beans.property.SimpleStringProperty(String.format("%.2f", param.getValue().getRating())));
    }

    /**
     * Загрузить дисциплины для выбранной группы
     */
    private void loadDisciplinesForGroup(int groupId) {
        try {
            ObservableList<Discipline> disciplines = FXCollections.observableArrayList(
                    disciplineService.getDisciplinesByGroup(groupId)
            );
            disciplineListView.setItems(disciplines);
            ratingsTable.getItems().clear();
        } catch (Exception e) {
            logger.error("Error loading disciplines", e);
            showError("Ошибка загрузки дисциплин");
        }
    }

    /**
     * Загрузить рейтинги для выбранной дисциплины
     */
    private void loadRatingsForDiscipline(int disciplineId) {
        try {
            logger.info("Loading ratings for discipline: {}", disciplineId);
            ObservableList<Rating> ratings = FXCollections.observableArrayList(
                    ratingService.getRatingsByDiscipline(disciplineId)
            );
            logger.info("Loaded {} ratings", ratings.size());
            ratingsTable.setItems(ratings);
        } catch (Exception e) {
            logger.error("Error loading ratings", e);
            showError("Ошибка загрузки рейтингов");
        }
    }

    @FXML
    private void handleAddDiscipline() {
        if (!canModifyData()) {
            showError("У вас нет прав для этого действия");
            return;
        }

        Group selectedGroup = groupListView.getSelectionModel().getSelectedItem();
        if (selectedGroup == null) {
            showError("Пожалуйста, выберите группу");
            return;
        }

        Dialog<String> dialog = new Dialog<>();
        styleDialog(dialog);
        dialog.setTitle("Добавить дисциплину");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField disciplineCodeField = new TextField();
        disciplineCodeField.setPromptText("Код дисциплины (например, МАТ)");

        grid.add(new Label("Код дисциплины:"), 0, 0);
        grid.add(disciplineCodeField, 1, 0);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Предотвращаем закрытие окна при ошибке валидации
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            String code = disciplineCodeField.getText().trim();
            if (!ValidationUtils.isValidDisciplineCode(code)) {
                showError("Неверный код дисциплины");
                event.consume();
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return disciplineCodeField.getText().trim();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(code -> {
            try {
                // Проверить лимит дисциплин
                int currentCount = disciplineService.getDisciplinesByGroup(selectedGroup.getId()).size();
                if (currentCount >= selectedGroup.getDisciplineCount()) {
                    showError("Ошибка: Превышен лимит дисциплин для этой группы (макс: " + selectedGroup.getDisciplineCount() + ")");
                    return;
                }

                Discipline discipline = new Discipline(selectedGroup.getId(), code);
                disciplineService.createDiscipline(discipline);
                loadDisciplinesForGroup(selectedGroup.getId());
                showInfo("Дисциплина \"" + code + "\" добавлена");
                logger.info("Discipline created: {}", code);
            } catch (Exception e) {
                logger.error("Error creating discipline", e);
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("duplicate key")) {
                    showError("Ошибка: Дисциплина с таким кодом уже существует в этой группе.");
                } else {
                    showError("Ошибка при добавлении дисциплины: " + errorMsg);
                }
            }
        });
    }

    @FXML
    private void handleDeleteDiscipline() {
        if (!canModifyData()) {
            showError("У вас нет прав для этого действия");
            return;
        }

        Discipline selectedDiscipline = disciplineListView.getSelectionModel().getSelectedItem();
        if (selectedDiscipline == null) {
            showError("Пожалуйста, выберите дисциплину для удаления");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        styleDialog(confirm);
        confirm.setTitle("Подтверждение");
        confirm.setContentText("Удалить дисциплину \"" + selectedDiscipline.getDisciplineCode() + "\"?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                disciplineService.deleteDiscipline(selectedDiscipline.getId());
                Group selectedGroup = groupListView.getSelectionModel().getSelectedItem();
                if (selectedGroup != null) {
                    loadDisciplinesForGroup(selectedGroup.getId());
                }
                ratingsTable.getItems().clear();
                showInfo("Дисциплина удалена");
                logger.info("Discipline deleted: {}", selectedDiscipline.getId());
            } catch (Exception e) {
                logger.error("Error deleting discipline", e);
                showError("Ошибка при удалении дисциплины");
            }
        }
    }

    @FXML
    private void handleEditDiscipline() {
        if (!canModifyData()) {
            showError("У вас нет прав для этого действия");
            return;
        }

        Discipline selectedDiscipline = disciplineListView.getSelectionModel().getSelectedItem();
        if (selectedDiscipline == null) {
            showError("Пожалуйста, выберите дисциплину для редактирования");
            return;
        }

        Dialog<String> dialog = new Dialog<>();
        styleDialog(dialog);
        dialog.setTitle("Редактировать дисциплину");
        dialog.setHeaderText("Изменение названия дисциплины");

        TextField codeField = new TextField(selectedDiscipline.getDisciplineCode());
        codeField.setPromptText("Новое название");
        
        VBox content = new VBox(10, new Label("Новое название:"), codeField);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Предотвращаем закрытие окна при ошибке валидации
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            String code = codeField.getText().trim();
            if (!ValidationUtils.isValidDisciplineCode(code)) {
                showError("Неверный код дисциплины");
                event.consume();
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return codeField.getText().trim();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(code -> {
            try {
                selectedDiscipline.setDisciplineCode(code);
                disciplineService.updateDiscipline(selectedDiscipline);
                
                Group selectedGroup = groupListView.getSelectionModel().getSelectedItem();
                if (selectedGroup != null) {
                    loadDisciplinesForGroup(selectedGroup.getId());
                }
                
                showInfo("Дисциплина обновлена");
                logger.info("Discipline updated: {}", code);
            } catch (Exception e) {
                logger.error("Error updating discipline", e);
                showError("Ошибка при обновлении дисциплины: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleAddRating() {
        if (!canModifyData()) {
            showError("У вас нет прав для этого действия");
            return;
        }

        Discipline selectedDiscipline = disciplineListView.getSelectionModel().getSelectedItem();
        if (selectedDiscipline == null) {
            showError("Пожалуйста, выберите дисциплину");
            return;
        }

        Group selectedGroup = groupListView.getSelectionModel().getSelectedItem();
        if (selectedGroup == null) {
            showError("Пожалуйста, выберите группу");
            return;
        }

        Dialog<Rating> dialog = new Dialog<>();
        styleDialog(dialog);
        dialog.setTitle("Добавить рейтинг студента");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        // Номер студента - расширяем диапазон, чтобы избежать авто-коррекции (clamping)
        Spinner<Integer> studentNumSpinner = new Spinner<>(-999999, 999999, 1);
        studentNumSpinner.setEditable(true);
        
        // ФИО студента
        TextField studentNameField = new TextField();
        studentNameField.setPromptText("Введите ФИО студента");
        
        // Рейтинг - расширяем диапазон, чтобы избежать авто-коррекции (clamping)
        Spinner<Double> ratingSpinner = new Spinner<>(-999999.0, 999999.0, 50.0, 0.5);
        ratingSpinner.setEditable(true);
        
        grid.add(new Label("Номер студента:"), 0, 0);
        grid.add(studentNumSpinner, 1, 0);
        grid.add(new Label("ФИО студента:"), 0, 1);
        grid.add(studentNameField, 1, 1);
        grid.add(new Label("Рейтинг (0-100):"), 0, 2);
        grid.add(ratingSpinner, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Предотвращаем закрытие окна при ошибке валидации
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                String studentNumText = studentNumSpinner.getEditor().getText().trim();
                String ratingText = ratingSpinner.getEditor().getText().trim().replace(',', '.');
                String studentName = studentNameField.getText().trim();

                int studentNum = Integer.parseInt(studentNumText);
                double rating = Double.parseDouble(ratingText);

                if (studentNum < 1 || studentNum > selectedGroup.getStudentCount()) {
                    showError("Ошибка: Номер студента должен быть от 1 до " + selectedGroup.getStudentCount());
                    event.consume();
                    return;
                }
                if (rating < 0 || rating > 100) {
                    showError("Ошибка: Рейтинг должен быть строго в диапазоне от 0 до 100");
                    event.consume();
                    return;
                }
                if (studentName.isEmpty()) {
                    showError("Пожалуйста, введите ФИО студента");
                    event.consume();
                }
            } catch (NumberFormatException e) {
                showError("Ошибка: Введены некорректные символы. Пожалуйста, используйте только цифры.");
                event.consume();
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                int studentNum = Integer.parseInt(studentNumSpinner.getEditor().getText().trim());
                double rating = Double.parseDouble(ratingSpinner.getEditor().getText().trim().replace(',', '.'));
                String studentName = studentNameField.getText().trim();
                return new Rating(selectedDiscipline.getId(), studentNum, studentName, rating);
            }
            return null;
        });

        Optional<Rating> result = dialog.showAndWait();
        result.ifPresent(rating -> {
            try {
                // Проверить, существует ли уже рейтинг для этого студента
                Rating existing = ratingService.getRatingByStudent(rating.getDisciplineId(), rating.getStudentNumber());
                
                if (existing != null) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    styleDialog(confirm);
                    confirm.setTitle("Рейтинг уже существует");
                    confirm.setHeaderText("Студент №" + rating.getStudentNumber() + " уже имеет оценку.");
                    confirm.setContentText("Вы хотите обновить существующую оценку (" + existing.getRating() + ") на новую (" + rating.getRating() + ")?");
                    
                    Optional<ButtonType> confirmResult = confirm.showAndWait();
                    if (confirmResult.isPresent() && confirmResult.get() == ButtonType.OK) {
                        existing.setRating(rating.getRating());
                        existing.setStudentName(rating.getStudentName());
                        ratingService.updateRating(existing);
                        loadRatingsForDiscipline(selectedDiscipline.getId());
                        showInfo("Рейтинг обновлен");
                        logger.info("Rating updated for student {} in discipline {}", rating.getStudentName(), selectedDiscipline.getId());
                    }
                    return;
                }

                ratingService.addRating(rating);
                loadRatingsForDiscipline(selectedDiscipline.getId());
                showInfo("Рейтинг добавлен: " + rating.getStudentName() + " - " + rating.getRating());
                logger.info("Rating added for student {} in discipline {}", rating.getStudentName(), selectedDiscipline.getId());
            } catch (Exception e) {
                logger.error("Error adding rating", e);
                String errorMsg = e.getMessage();
                showError("Ошибка при добавлении рейтинга: " + errorMsg);
            }
        });
    }

    @FXML
    private void handleEditRating() {
        if (!canModifyData()) {
            showError("У вас нет прав для этого действия");
            return;
        }

        Rating selectedRating = ratingsTable.getSelectionModel().getSelectedItem();
        if (selectedRating == null) {
            showError("Пожалуйста, выберите рейтинг для редактирования");
            return;
        }

        Discipline selectedDiscipline = disciplineListView.getSelectionModel().getSelectedItem();

        Dialog<Rating> dialog = new Dialog<>();
        styleDialog(dialog);
        dialog.setTitle("Редактировать рейтинг");
        dialog.setHeaderText("Редактирование данных студента №" + selectedRating.getStudentNumber());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField(selectedRating.getStudentName());
        nameField.setPromptText("ФИО студента");

        // Рейтинг - расширяем диапазон, чтобы избежать авто-коррекции (clamping)
        Spinner<Double> ratingSpinner = new Spinner<>(-999999.0, 999999.0, selectedRating.getRating(), 0.5);
        ratingSpinner.setEditable(true);

        grid.add(new Label("ФИО студента:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Рейтинг (0-100):"), 0, 1);
        grid.add(ratingSpinner, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Предотвращаем закрытие окна при ошибке валидации
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                String newName = nameField.getText().trim();
                String ratingText = ratingSpinner.getEditor().getText().trim().replace(',', '.');
                double newRatingValue = Double.parseDouble(ratingText);

                if (newName.isEmpty()) {
                    showError("ФИО не может быть пустым");
                    event.consume();
                    return;
                }

                if (newRatingValue < 0 || newRatingValue > 100) {
                    showError("Ошибка: Рейтинг должен быть строго в диапазоне от 0 до 100");
                    event.consume();
                }
            } catch (NumberFormatException e) {
                showError("Ошибка: Введены некорректные символы в поле рейтинга.");
                event.consume();
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                String newName = nameField.getText().trim();
                double newRatingValue = Double.parseDouble(ratingSpinner.getEditor().getText().trim().replace(',', '.'));
                selectedRating.setStudentName(newName);
                selectedRating.setRating(newRatingValue);
                return selectedRating;
            }
            return null;
        });

        Optional<Rating> result = dialog.showAndWait();
        result.ifPresent(updatedRating -> {
            try {
                ratingService.updateRating(updatedRating);
                loadRatingsForDiscipline(selectedDiscipline.getId());
                showInfo("Данные обновлены");
                logger.info("Rating updated for student {} in discipline {}", updatedRating.getStudentName(), selectedDiscipline.getId());
            } catch (Exception e) {
                logger.error("Error updating rating", e);
                showError("Ошибка при обновлении: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleDeleteRating() {
        if (!canModifyData()) {
            showError("У вас нет прав для этого действия");
            return;
        }

        Rating selectedRating = ratingsTable.getSelectionModel().getSelectedItem();
        if (selectedRating == null) {
            showError("Пожалуйста, выберите рейтинг для удаления");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        styleDialog(confirm);
        confirm.setTitle("Подтверждение");
        confirm.setContentText("Удалить рейтинг студента № " + selectedRating.getStudentNumber() + "?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                ratingService.deleteRating(selectedRating.getId());
                
                Discipline selectedDiscipline = disciplineListView.getSelectionModel().getSelectedItem();
                if (selectedDiscipline != null) {
                    loadRatingsForDiscipline(selectedDiscipline.getId());
                }
                showInfo("Рейтинг удалён");
                logger.info("Rating deleted: {}", selectedRating.getId());
            } catch (Exception e) {
                logger.error("Error deleting rating", e);
                showError("Ошибка при удалении рейтинга: " + e.getMessage());
            }
        }
    }

    /**
     * Проверить, может ли текущий пользователь изменять данные
     */
    private boolean canModifyData() {
        if (currentUser == null) {
            return false;
        }
        return currentUser.getRole() == User.UserRole.ADMINISTRATOR ||
               currentUser.getRole() == User.UserRole.MODERATOR;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        styleDialog(alert);
        alert.setTitle("Ошибка");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        styleDialog(alert);
        alert.setTitle("Информация");
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleImportFromPDF() {
        if (!canModifyData()) {
            showError("У вас нет прав для импорта данных");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите PDF отчет для импорта");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File selectedFile = fileChooser.showOpenDialog(tabPane.getScene().getWindow());

        if (selectedFile != null) {
            try {
                PDFImporter.ImportedData data = PDFImporter.importDisciplineRatings(selectedFile.getAbsolutePath());
                
                // Показать превью данных
                StringBuilder preview = new StringBuilder();
                preview.append("Группа: ").append(data.groupCode).append("\n");
                preview.append("Дисциплина: ").append(data.disciplineName).append("\n");
                preview.append("Найдено студентов: ").append(data.students.size()).append("\n\n");
                preview.append("Вы хотите импортировать эти данные?");

                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                styleDialog(confirm);
                confirm.setTitle("Подтверждение импорта");
                confirm.setHeaderText("Обнаружены данные в PDF");
                confirm.setContentText(preview.toString());

                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    processImportedData(data);
                }
            } catch (Exception e) {
                logger.error("Error importing PDF", e);
                String errorMsg = e.getMessage();
                if (errorMsg == null || errorMsg.isEmpty()) errorMsg = e.toString();
                showError("Ошибка при чтении PDF: " + errorMsg + "\n\nУбедитесь, что вы выбираете файл 'Рейтинги студентов по дисциплине', а не сводку.");
            }
        }
    }

    private void processImportedData(PDFImporter.ImportedData data) {
        try {
            // 1. Найти или создать группу
            Group group = groupService.getGroupByCode(data.groupCode);
            if (group == null) {
                group = new Group(0, data.groupCode, data.students.size(), 1);
                groupService.createGroup(group);
                group = groupService.getGroupByCode(data.groupCode);
                logger.info("Created new group during import: {}", data.groupCode);
            }

            // 2. Найти или создать дисциплину
            List<Discipline> disciplines = disciplineService.getDisciplinesByGroup(group.getId());
            Discipline targetDisc = null;
            for (Discipline d : disciplines) {
                if (d.getDisciplineCode().equalsIgnoreCase(data.disciplineName)) {
                    targetDisc = d;
                    break;
                }
            }

            if (targetDisc == null) {
                targetDisc = new Discipline(0, group.getId(), data.disciplineName);
                disciplineService.createDiscipline(targetDisc);
                // Перезагружаем чтобы получить ID
                disciplines = disciplineService.getDisciplinesByGroup(group.getId());
                for (Discipline d : disciplines) {
                    if (d.getDisciplineCode().equalsIgnoreCase(data.disciplineName)) {
                        targetDisc = d;
                        break;
                    }
                }
            }

            // 3. Импорт рейтингов
            int importedCount = 0;
            for (int i = 0; i < data.students.size(); i++) {
                String[] student = data.students.get(i);
                String name = student[0];
                double ratingVal = Double.parseDouble(student[1]);
                int studentNum = i + 1;

                Rating rating = new Rating(targetDisc.getId(), studentNum, name, ratingVal);
                
                // Проверяем существование
                Rating existing = ratingService.getRatingByStudent(targetDisc.getId(), studentNum);
                if (existing != null) {
                    existing.setRating(ratingVal);
                    existing.setStudentName(name);
                    ratingService.updateRating(existing);
                } else {
                    ratingService.addRating(rating);
                }
                importedCount++;
            }

            loadGroups();
            showInfo("Успешно импортировано " + importedCount + " записей для группы " + data.groupCode);
            logger.info("Imported {} ratings from PDF", importedCount);

        } catch (Exception e) {
            logger.error("Error processing imported data", e);
            showError("Ошибка при сохранении данных: " + e.getMessage());
        }
    }

    /**
     * Выход в меню входа
     */
    @FXML
    private void handleBackToLogin() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        styleDialog(confirm);
        confirm.setTitle("Выход");
        confirm.setHeaderText("Вы уверены?");
        confirm.setContentText("Вы действительно хотите выйти из приложения?");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/LoginWindow.fxml"));
                Parent root = loader.load();
                
                Stage loginStage = new Stage();
                loginStage.setTitle("Rating System - Вход в систему");
                loginStage.setScene(new Scene(root, 600, 400));
                loginStage.show();
                
                Stage mainStage = (Stage) tabPane.getScene().getWindow();
                mainStage.close();
                
                logger.info("User logged out: {}", currentUser.getUsername());
            } catch (Exception e) {
                logger.error("Error logging out", e);
                showError("Ошибка при выходе");
            }
        }
    }

    /**
     * Завершить приложение
     */
    @FXML
    private void handleExit() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        styleDialog(confirm);
        confirm.setTitle("Завершение");
        confirm.setHeaderText("Завершить приложение?");
        confirm.setContentText("Вы уверены, что хотите завершить приложение?");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            logger.info("Application terminated by user");
            System.exit(0);
        }
    }

    /**
     * Обработчик для обновления содержимого при смене вкладок
     */
    @FXML
    private void handleTabChanged() {
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null && selectedTab.getText().equals("Содержимое файла")) {
            loadFileContent();
        }
    }

    /**
     * Загрузить содержимое файла
     */
    private void loadFileContent() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Информация о системе ===\n");
            sb.append("Пользователь: ").append(currentUser.getUsername()).append("\n");
            sb.append("Роль: ").append(currentUser.getRole().getDisplayName()).append("\n\n");
            
            sb.append("=== Группы в системе ===\n");
            for (Group group : groupsList) {
                sb.append("Группа: ").append(group.getGroupCode()).append("\n");
                sb.append("  Студентов: ").append(group.getStudentCount()).append("\n");
                sb.append("  Дисциплин: ").append(group.getDisciplineCount()).append("\n");
                
                List<Discipline> disciplines = disciplineService.getDisciplinesByGroup(group.getId());
                for (Discipline d : disciplines) {
                    sb.append("    Дисциплина: ").append(d.getDisciplineCode()).append("\n");
                    List<Rating> ratings = ratingService.getRatingsByDiscipline(d.getId());
                    for (Rating r : ratings) {
                        sb.append("      - Студент №").append(r.getStudentNumber())
                          .append(" (").append(r.getStudentName()).append("): ")
                          .append(r.getRating()).append("\n");
                    }
                }
                sb.append("\n");
            }
            
            contentArea.setText(sb.toString());
        } catch (Exception e) {
            logger.error("Error loading file content", e);
        }
    }
}
