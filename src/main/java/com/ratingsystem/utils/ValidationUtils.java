package com.ratingsystem.utils;

/**
 * Утилиты для валидации данных
 */
public class ValidationUtils {

    /**
     * Проверить валидность кода группы
     */
    public static boolean isValidGroupCode(String code) {
        return code != null && !code.trim().isEmpty() && code.length() <= 50;
    }

    /**
     * Проверить валидность кода дисциплины
     */
    public static boolean isValidDisciplineCode(String code) {
        return code != null && !code.trim().isEmpty() && code.length() <= 50;
    }

    /**
     * Проверить валидность количества студентов
     */
    public static boolean isValidStudentCount(int count) {
        return count > 0 && count <= 300;
    }

    /**
     * Проверить валидность количества дисциплин
     */
    public static boolean isValidDisciplineCount(int count) {
        return count > 0 && count <= 8;
    }

    /**
     * Проверить валидность рейтинга
     */
    public static boolean isValidRating(double rating) {
        return rating >= 0 && rating <= 100;
    }

    /**
     * Проверить валидность номера студента
     */
    public static boolean isValidStudentNumber(int number, int totalStudents) {
        return number > 0 && number <= totalStudents;
    }

    /**
     * Проверить валидность имени пользователя
     */
    public static boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        return username.matches("^[a-zA-Z0-9_]{3,20}$");
    }
}
