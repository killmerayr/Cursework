package com.ratingsystem.database;

import com.ratingsystem.models.Discipline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для работы с дисциплинами
 */
public class DisciplineService {

    private static final Logger logger = LoggerFactory.getLogger(DisciplineService.class);
    private DatabaseManager db;

    public DisciplineService() {
        this.db = DatabaseManager.getInstance();
    }

    /**
     * Получить все дисциплины для группы
     */
    public List<Discipline> getDisciplinesByGroup(int groupId) {
        List<Discipline> disciplines = new ArrayList<>();
        try {
            ResultSet rs = db.executeQuery(
                    "SELECT id, group_id, discipline_code FROM disciplines WHERE group_id = ? ORDER BY discipline_code",
                    groupId
            );
            while (rs.next()) {
                Discipline discipline = new Discipline(
                        rs.getInt("id"),
                        rs.getInt("group_id"),
                        rs.getString("discipline_code")
                );
                disciplines.add(discipline);
            }
        } catch (Exception e) {
            logger.error("Error getting disciplines for group: {}", groupId, e);
        }
        return disciplines;
    }

    /**
     * Получить дисциплину по ID
     */
    public Discipline getDisciplineById(int id) {
        try {
            ResultSet rs = db.executeQuery(
                    "SELECT id, group_id, discipline_code FROM disciplines WHERE id = ?",
                    id
            );
            if (rs.next()) {
                return new Discipline(
                        rs.getInt("id"),
                        rs.getInt("group_id"),
                        rs.getString("discipline_code")
                );
            }
        } catch (Exception e) {
            logger.error("Error getting discipline by id: {}", id, e);
        }
        return null;
    }

    /**
     * Добавить новую дисциплину
     */
    public void createDiscipline(Discipline discipline) throws Exception {
        db.executeUpdate(
                "INSERT INTO disciplines (group_id, discipline_code) VALUES (?, ?)",
                discipline.getGroupId(),
                discipline.getDisciplineCode()
        );
        logger.info("Discipline created: {} for group {}", discipline.getDisciplineCode(), discipline.getGroupId());
    }

    /**
     * Удалить дисциплину
     */
    public void deleteDiscipline(int id) throws Exception {
        // Удалить рейтинги
        db.executeUpdate("DELETE FROM ratings WHERE discipline_id = ?", id);
        
        // Удалить сводки
        db.executeUpdate("DELETE FROM summaries WHERE discipline_id = ?", id);
        
        // Удалить дисциплину
        db.executeUpdate("DELETE FROM disciplines WHERE id = ?", id);
        logger.info("Discipline deleted: {}", id);
    }

    /**
     * Обновить дисциплину
     */
    public void updateDiscipline(Discipline discipline) throws Exception {
        db.executeUpdate(
                "UPDATE disciplines SET discipline_code = ? WHERE id = ?",
                discipline.getDisciplineCode(),
                discipline.getId()
        );
        logger.info("Discipline updated: {} (ID: {})", discipline.getDisciplineCode(), discipline.getId());
    }

    /**
     * Получить количество дисциплин для группы
     */
    public int getDisciplineCount(int groupId) {
        try {
            ResultSet rs = db.executeQuery(
                    "SELECT COUNT(*) as count FROM disciplines WHERE group_id = ?",
                    groupId
            );
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (Exception e) {
            logger.error("Error getting discipline count for group: {}", groupId, e);
        }
        return 0;
    }
}
