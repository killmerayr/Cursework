package com.ratingsystem.models;

/**
 * Модель дисциплины
 */
public class Discipline {

    private int id;
    private int groupId;
    private String disciplineCode;
    private String createdAt;

    public Discipline() {
    }

    public Discipline(int groupId, String disciplineCode) {
        this.groupId = groupId;
        this.disciplineCode = disciplineCode;
    }

    public Discipline(int id, int groupId, String disciplineCode) {
        this.id = id;
        this.groupId = groupId;
        this.disciplineCode = disciplineCode;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public String getDisciplineCode() {
        return disciplineCode;
    }

    public void setDisciplineCode(String disciplineCode) {
        this.disciplineCode = disciplineCode;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return disciplineCode;
    }
}
