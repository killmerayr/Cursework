package com.ratingsystem.database;

import com.ratingsystem.models.User;
import com.ratingsystem.utils.PasswordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для работы с пользователями
 */
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private DatabaseManager db;

    public UserService() {
        this.db = DatabaseManager.getInstance();
    }

    /**
     * Инициализировать администратора по умолчанию
     */
    public void initializeDefaultAdmin() {
        try {
            ResultSet rs = db.executeQuery("SELECT id, password_hash FROM users WHERE username = 'admin'");
            
            if (!rs.next()) {
                createUser("admin", "admin123", User.UserRole.ADMINISTRATOR);
                logger.info("Default admin user created");
            } else {
                String currentHash = rs.getString("password_hash");
                // Если хеш старый (не BCrypt), обновляем его до BCrypt для безопасности
                if (currentHash == null || !currentHash.startsWith("$2a$")) {
                    String newHash = PasswordUtils.hashPassword("admin123");
                    db.executeUpdate("UPDATE users SET password_hash = ? WHERE username = 'admin'", newHash);
                    logger.info("Default admin password migrated to BCrypt");
                }
            }
        } catch (Exception e) {
            logger.error("Error initializing default admin", e);
        }
    }

    /**
     * Аутентифицировать пользователя
     */
    public User authenticate(String username, String password) {
        try {
            ResultSet rs = db.executeQuery(
                    "SELECT id, username, password_hash, role FROM users WHERE username = ?",
                    username
            );

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                if (PasswordUtils.verifyPassword(password, storedHash)) {
                    User user = new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            storedHash,
                            User.UserRole.fromString(rs.getString("role"))
                    );
                    logger.info("User authenticated: {}", username);
                    return user;
                }
            }
        } catch (Exception e) {
            logger.error("Error authenticating user", e);
        }
        return null;
    }

    /**
     * Получить пользователя по имени
     */
    public User getUserByUsername(String username) {
        try {
            ResultSet rs = db.executeQuery(
                    "SELECT id, username, password_hash, role FROM users WHERE username = ?",
                    username
            );

            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        User.UserRole.fromString(rs.getString("role"))
                );
            }
        } catch (Exception e) {
            logger.error("Error getting user by username", e);
        }
        return null;
    }

    /**
     * Создать нового пользователя
     */
    public void createUser(String username, String password, User.UserRole role) throws Exception {
        String passwordHash = PasswordUtils.hashPassword(password);
        db.executeUpdate(
                "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)",
                username,
                passwordHash,
                role.name()
        );
        logger.info("User created: {} with role {}", username, role);
    }

    /**
     * Проверить существование пользователя
     */
    public boolean userExists(String username) {
        try {
            ResultSet rs = db.executeQuery("SELECT id FROM users WHERE username = ?", username);
            return rs.next();
        } catch (Exception e) {
            logger.error("Error checking user existence", e);
            return false;
        }
    }

    /**
     * Получить всех пользователей
     */
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        try {
            ResultSet rs = db.executeQuery(
                    "SELECT id, username, password_hash, role FROM users ORDER BY username"
            );
            while (rs.next()) {
                User user = new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        User.UserRole.fromString(rs.getString("role"))
                );
                users.add(user);
            }
        } catch (Exception e) {
            logger.error("Error getting all users", e);
        }
        return users;
    }

    /**
     * Удалить пользователя
     */
    public void deleteUser(int userId) throws Exception {
        db.executeUpdate("DELETE FROM users WHERE id = ?", userId);
        logger.info("User deleted: {}", userId);
    }

    /**
     * Изменить роль пользователя (только для администратора)
     */
    public int updateUserRole(int userId, User.UserRole newRole) {
        try {
            int result = db.executeUpdate(
                    "UPDATE users SET role = ? WHERE id = ?",
                    newRole.name(),
                    userId
            );
            logger.info("User role updated: userId={}, newRole={}", userId, newRole);
            return result;
        } catch (Exception e) {
            logger.error("Error updating user role", e);
            return 0;
        }
    }
}
