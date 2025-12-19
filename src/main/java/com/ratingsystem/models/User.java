package com.ratingsystem.models;

/**
 * Модель пользователя системы
 */
public class User {

    private int id;
    private String username;
    private String passwordHash;
    private UserRole role;

    public enum UserRole {
        ADMINISTRATOR("Administrator"),
        MODERATOR("Moderator"),
        GUEST("Guest");

        private final String displayName;

        UserRole(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static UserRole fromString(String role) {
            if (role == null) return GUEST;
            String r = role.trim().toUpperCase();
            if (r.equals("ADMIN") || r.equals("ADMINISTRATOR")) {
                return ADMINISTRATOR;
            }
            if (r.equals("MODERATOR") || r.equals("MODER")) {
                return MODERATOR;
            }
            for (UserRole ur : UserRole.values()) {
                if (ur.name().equalsIgnoreCase(role)) {
                    return ur;
                }
            }
            return GUEST;
        }
    }

    public User() {
    }

    public User(int id, String username, String passwordHash, UserRole role) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", role=" + role +
                '}';
    }
}
