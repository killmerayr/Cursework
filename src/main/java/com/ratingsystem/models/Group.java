package com.ratingsystem.models;

/**
 * Модель группы студентов
 */
public class Group {

    private int id;
    private String groupCode;
    private int studentCount;
    private int disciplineCount;
    private String createdAt;
    private String updatedAt;

    public Group() {
    }

    public Group(String groupCode, int studentCount, int disciplineCount) {
        this.groupCode = groupCode;
        this.studentCount = studentCount;
        this.disciplineCount = disciplineCount;
    }

    public Group(int id, String groupCode, int studentCount, int disciplineCount) {
        this.id = id;
        this.groupCode = groupCode;
        this.studentCount = studentCount;
        this.disciplineCount = disciplineCount;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public void setGroupCode(String groupCode) {
        this.groupCode = groupCode;
    }

    public int getStudentCount() {
        return studentCount;
    }

    public void setStudentCount(int studentCount) {
        this.studentCount = studentCount;
    }

    public int getDisciplineCount() {
        return disciplineCount;
    }

    public void setDisciplineCount(int disciplineCount) {
        this.disciplineCount = disciplineCount;
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
        return groupCode + " (" + studentCount + " студентов, " + disciplineCount + " дисциплин)";
    }
}
