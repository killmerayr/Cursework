package com.ratingsystem.models;

/**
 * Модель рейтинга студента по дисциплине
 */
public class Rating {

    private int id;
    private int disciplineId;
    private int studentNumber;
    private String studentName;  // ФИО студента
    private double rating;
    private String createdAt;
    private String updatedAt;

    public Rating() {
    }

    public Rating(int disciplineId, int studentNumber, String studentName, double rating) {
        this.disciplineId = disciplineId;
        this.studentNumber = studentNumber;
        this.studentName = studentName;
        this.rating = rating;
    }

    public Rating(int id, int disciplineId, int studentNumber, String studentName, double rating) {
        this.id = id;
        this.disciplineId = disciplineId;
        this.studentNumber = studentNumber;
        this.studentName = studentName;
        this.rating = rating;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDisciplineId() {
        return disciplineId;
    }

    public void setDisciplineId(int disciplineId) {
        this.disciplineId = disciplineId;
    }

    public int getStudentNumber() {
        return studentNumber;
    }

    public void setStudentNumber(int studentNumber) {
        this.studentNumber = studentNumber;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        if (rating < 0 || rating > 100) {
            throw new IllegalArgumentException("Rating must be between 0 and 100");
        }
        this.rating = rating;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return (studentName != null ? studentName : "Студент " + studentNumber) + ": " + rating;
    }
}
