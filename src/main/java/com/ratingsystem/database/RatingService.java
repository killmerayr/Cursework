package com.ratingsystem.database;

import com.ratingsystem.models.Rating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Сервис для работы с рейтингами
 */
public class RatingService {

    private static final Logger logger = LoggerFactory.getLogger(RatingService.class);
    private DatabaseManager db;

    public RatingService() {
        this.db = DatabaseManager.getInstance();
    }

    /**
     * Получить все рейтинги для дисциплины
     */
    public List<Rating> getRatingsByDiscipline(int disciplineId) {
        List<Rating> ratings = new ArrayList<>();
        try {
            logger.info("Getting ratings for discipline: {}", disciplineId);
            ResultSet rs = db.executeQuery(
                    "SELECT id, discipline_id, student_number, student_name, rating FROM ratings WHERE discipline_id = ? ORDER BY student_number",
                    disciplineId
            );
            int count = 0;
            while (rs.next()) {
                count++;
                Rating rating = new Rating(
                        rs.getInt("id"),
                        rs.getInt("discipline_id"),
                        rs.getInt("student_number"),
                        rs.getString("student_name"),
                        rs.getDouble("rating")
                );
                logger.debug("Loaded rating: student={}, name={}, rating={}", rating.getStudentNumber(), rating.getStudentName(), rating.getRating());
                ratings.add(rating);
            }
            rs.close();
            logger.info("Loaded {} ratings for discipline {}", count, disciplineId);
        } catch (Exception e) {
            logger.error("Error getting ratings for discipline: {}", disciplineId, e);
        }
        return ratings;
    }

    /**
     * Получить рейтинг конкретного студента по дисциплине
     */
    public Rating getRatingByStudent(int disciplineId, int studentNumber) {
        try {
            ResultSet rs = db.executeQuery(
                    "SELECT id, discipline_id, student_number, student_name, rating FROM ratings WHERE discipline_id = ? AND student_number = ?",
                    disciplineId, studentNumber
            );
            if (rs.next()) {
                return new Rating(
                        rs.getInt("id"),
                        rs.getInt("discipline_id"),
                        rs.getInt("student_number"),
                        rs.getString("student_name"),
                        rs.getDouble("rating")
                );
            }
        } catch (Exception e) {
            logger.error("Error getting rating by student", e);
        }
        return null;
    }

    /**
     * Добавить рейтинг студента
     */
    public void addRating(Rating rating) throws Exception {
        db.executeUpdate(
                "INSERT INTO ratings (discipline_id, student_number, student_name, rating) VALUES (?, ?, ?, ?)",
                rating.getDisciplineId(),
                rating.getStudentNumber(),
                rating.getStudentName(),
                rating.getRating()
        );
        updateSummary(rating.getDisciplineId());
        logger.info("Rating added: student {} discipline {}", rating.getStudentNumber(), rating.getDisciplineId());
    }

    /**
     * Обновить рейтинг студента
     */
    public void updateRating(Rating rating) throws Exception {
        db.executeUpdate(
                "UPDATE ratings SET rating = ?, student_name = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                rating.getRating(),
                rating.getStudentName(),
                rating.getId()
        );
        updateSummary(rating.getDisciplineId());
        logger.info("Rating updated: {}", rating.getId());
    }

    /**
     * Удалить рейтинг
     */
    public void deleteRating(int id) throws Exception {
        // Получить discipline_id перед удалением
        ResultSet rs = db.executeQuery("SELECT discipline_id FROM ratings WHERE id = ?", id);
        int disciplineId = 0;
        if (rs.next()) {
            disciplineId = rs.getInt("discipline_id");
        }
        
        db.executeUpdate("DELETE FROM ratings WHERE id = ?", id);
        if (disciplineId > 0) {
            updateSummary(disciplineId);
        }
        logger.info("Rating deleted: {}", id);
    }

    /**
     * Получить средний рейтинг по дисциплине
     */
    public double getAverageRating(int disciplineId) {
        try {
            ResultSet rs = db.executeQuery(
                    "SELECT AVG(rating) as avg_rating FROM ratings WHERE discipline_id = ?",
                    disciplineId
            );
            if (rs.next()) {
                double avg = rs.getDouble("avg_rating");
                return Double.isNaN(avg) ? 0.0 : avg;
            }
        } catch (Exception e) {
            logger.error("Error getting average rating for discipline: {}", disciplineId, e);
        }
        return 0.0;
    }

    /**
     * Получить сводку рейтингов по дисциплинам для группы
     */
    public Map<String, Double> getSummaryByGroup(int groupId) {
        Map<String, Double> summary = new HashMap<>();
        try {
            ResultSet rs = db.executeQuery(
                    "SELECT d.discipline_code, AVG(r.rating) as avg_rating " +
                    "FROM disciplines d " +
                    "LEFT JOIN ratings r ON d.id = r.discipline_id " +
                    "WHERE d.group_id = ? " +
                    "GROUP BY d.id, d.discipline_code " +
                    "ORDER BY d.discipline_code",
                    groupId
            );
            while (rs.next()) {
                String disciplineCode = rs.getString("discipline_code");
                double avgRating = rs.getDouble("avg_rating");
                summary.put(disciplineCode, Double.isNaN(avgRating) ? 0.0 : avgRating);
            }
        } catch (Exception e) {
            logger.error("Error getting summary by group: {}", groupId, e);
        }
        return summary;
    }

    /**
     * Обновить/создать сводку для дисциплины
     */
    private void updateSummary(int disciplineId) {
        try {
            // Получить информацию о дисциплине и группе
            ResultSet rs = db.executeQuery(
                    "SELECT d.group_id, d.id FROM disciplines d WHERE d.id = ?",
                    disciplineId
            );
            
            if (rs.next()) {
                int groupId = rs.getInt("group_id");
                double avgRating = getAverageRating(disciplineId);
                
                // Проверить, существует ли запись в summaries
                ResultSet checkRs = db.executeQuery(
                        "SELECT id FROM summaries WHERE group_id = ? AND discipline_id = ?",
                        groupId, disciplineId
                );
                
                if (checkRs.next()) {
                    // Обновить
                    db.executeUpdate(
                            "UPDATE summaries SET avg_rating = ? WHERE group_id = ? AND discipline_id = ?",
                            avgRating, groupId, disciplineId
                    );
                } else {
                    // Вставить
                    db.executeUpdate(
                            "INSERT INTO summaries (group_id, discipline_id, avg_rating) VALUES (?, ?, ?)",
                            groupId, disciplineId, avgRating
                    );
                }
            }
        } catch (Exception e) {
            logger.error("Error updating summary for discipline: {}", disciplineId, e);
        }
    }

    /**
     * Получить количество рейтингов для дисциплины
     */
    public int getRatingCount(int disciplineId) {
        try {
            ResultSet rs = db.executeQuery(
                    "SELECT COUNT(*) as count FROM ratings WHERE discipline_id = ?",
                    disciplineId
            );
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (Exception e) {
            logger.error("Error getting rating count for discipline: {}", disciplineId, e);
        }
        return 0;
    }
}
