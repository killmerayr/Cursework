package com.ratingsystem.database;

import com.ratingsystem.models.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для работы с группами
 */
public class GroupService {

    private static final Logger logger = LoggerFactory.getLogger(GroupService.class);
    private DatabaseManager db;

    public GroupService() {
        this.db = DatabaseManager.getInstance();
    }

    /**
     * Получить все группы
     */
    public List<Group> getAllGroups() {
        List<Group> groups = new ArrayList<>();
        try {
            ResultSet rs = db.executeQuery(
                    "SELECT id, group_code, student_count, discipline_count FROM groups ORDER BY group_code"
            );
            while (rs.next()) {
                Group group = new Group(
                        rs.getInt("id"),
                        rs.getString("group_code"),
                        rs.getInt("student_count"),
                        rs.getInt("discipline_count")
                );
                groups.add(group);
            }
        } catch (Exception e) {
            logger.error("Error getting all groups", e);
        }
        return groups;
    }

    /**
     * Получить группу по ID
     */
    public Group getGroupById(int id) {
        try {
            ResultSet rs = db.executeQuery(
                    "SELECT id, group_code, student_count, discipline_count FROM groups WHERE id = ?",
                    id
            );
            if (rs.next()) {
                return new Group(
                        rs.getInt("id"),
                        rs.getString("group_code"),
                        rs.getInt("student_count"),
                        rs.getInt("discipline_count")
                );
            }
        } catch (Exception e) {
            logger.error("Error getting group by id: {}", id, e);
        }
        return null;
    }

    /**
     * Получить группу по коду
     */
    public Group getGroupByCode(String code) {
        try {
            ResultSet rs = db.executeQuery(
                    "SELECT id, group_code, student_count, discipline_count FROM groups WHERE group_code = ?",
                    code
            );
            if (rs.next()) {
                return new Group(
                        rs.getInt("id"),
                        rs.getString("group_code"),
                        rs.getInt("student_count"),
                        rs.getInt("discipline_count")
                );
            }
        } catch (Exception e) {
            logger.error("Error getting group by code: {}", code, e);
        }
        return null;
    }

    /**
     * Добавить новую группу
     */
    public void createGroup(Group group) throws Exception {
        db.executeUpdate(
                "INSERT INTO groups (group_code, student_count, discipline_count) VALUES (?, ?, ?)",
                group.getGroupCode(),
                group.getStudentCount(),
                group.getDisciplineCount()
        );
        logger.info("Group created: {}", group.getGroupCode());
    }

    /**
     * Обновить группу
     */
    public void updateGroup(Group group) throws Exception {
        db.executeUpdate(
                "UPDATE groups SET student_count = ?, discipline_count = ? WHERE id = ?",
                group.getStudentCount(),
                group.getDisciplineCount(),
                group.getId()
        );
        logger.info("Group updated: {}", group.getGroupCode());
    }

    /**
     * Удалить группу по ID
     */
    public void deleteGroupById(int groupId) throws Exception {
        // Удалить рейтинги
        ResultSet disciplines = db.executeQuery(
                "SELECT id FROM disciplines WHERE group_id = ?", groupId
        );
        while (disciplines.next()) {
            db.executeUpdate("DELETE FROM ratings WHERE discipline_id = ?", disciplines.getInt("id"));
        }
        
        // Удалить дисциплины
        db.executeUpdate("DELETE FROM disciplines WHERE group_id = ?", groupId);
        
        // Удалить сводки
        db.executeUpdate("DELETE FROM summaries WHERE group_id = ?", groupId);
        
        // Удалить группу
        db.executeUpdate("DELETE FROM groups WHERE id = ?", groupId);
        logger.info("Group deleted: ID {}", groupId);
    }

    /**
     * Удалить группу по коду
     */
    public void deleteGroupByCode(String code) throws Exception {
        // Сначала удалить все связанные данные
        ResultSet rs = db.executeQuery("SELECT id FROM groups WHERE group_code = ?", code);
        if (rs.next()) {
            int groupId = rs.getInt("id");
            
            // Удалить рейтинги
            ResultSet disciplines = db.executeQuery(
                    "SELECT id FROM disciplines WHERE group_id = ?", groupId
            );
            while (disciplines.next()) {
                db.executeUpdate("DELETE FROM ratings WHERE discipline_id = ?", disciplines.getInt("id"));
            }
            
            // Удалить дисциплины
            db.executeUpdate("DELETE FROM disciplines WHERE group_id = ?", groupId);
            
            // Удалить сводки
            db.executeUpdate("DELETE FROM summaries WHERE group_id = ?", groupId);
            
            // Удалить группу
            db.executeUpdate("DELETE FROM groups WHERE id = ?", groupId);
            logger.info("Group deleted: {}", code);
        }
    }

    /**
     * Получить количество групп
     */
    public int getGroupCount() {
        try {
            ResultSet rs = db.executeQuery("SELECT COUNT(*) as count FROM groups");
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (Exception e) {
            logger.error("Error getting group count", e);
        }
        return 0;
    }
}
