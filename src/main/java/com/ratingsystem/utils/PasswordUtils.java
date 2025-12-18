package com.ratingsystem.utils;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Утилиты для работы с паролями с использованием BCrypt
 */
public class PasswordUtils {

    /**
     * Хешировать пароль с использованием BCrypt
     */
    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    /**
     * Проверить пароль против хеша
     */
    public static boolean verifyPassword(String password, String hash) {
        try {
            if (hash == null || !hash.startsWith("$2a$")) {
                return false;
            }
            return BCrypt.checkpw(password, hash);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Проверить сложность пароля
     * Требования: минимум 8 символов, хотя бы одна цифра и заглавная буква
     */
    public static boolean isStrongPassword(String password) {
        if (password.length() < 8) {
            return false;
        }
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasUpperCase = password.matches(".*[A-Z].*");
        return hasDigit && hasUpperCase;
    }
}
