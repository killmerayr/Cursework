package com.ratingsystem.utils;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Утилита для чтения и парсинга данных из PDF отчетов
 */
public class PDFImporter {

    private static final Logger logger = LoggerFactory.getLogger(PDFImporter.class);

    /**
     * Результат парсинга PDF
     */
    public static class ImportedData {
        public String groupCode;
        public String disciplineName;
        public List<String[]> students = new ArrayList<>(); // [ФИО, Рейтинг]
    }

    /**
     * Прочитать данные из PDF отчета по дисциплине
     */
    public static ImportedData importDisciplineRatings(String filePath) throws Exception {
        ImportedData data = new ImportedData();
        PdfReader reader = new PdfReader(filePath);
        
        StringBuilder textBuilder = new StringBuilder();
        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            textBuilder.append(PdfTextExtractor.getTextFromPage(reader, i));
            textBuilder.append("\n");
        }
        reader.close();
        
        String fullText = textBuilder.toString();
        logger.info("Extracted text length: {}", fullText.length());

        if (fullText.contains("Сводка рейтингов по группе")) {
            throw new Exception("Этот файл является сводкой. Импорт возможен только из отчетов по конкретной дисциплине.");
        }

        // Парсинг группы (более гибкий поиск)
        Pattern groupPattern = Pattern.compile("(?:Группа:|отчёт по группе)\\s*([^\\n\\r]*)", Pattern.CASE_INSENSITIVE);
        Matcher groupMatcher = groupPattern.matcher(fullText);
        if (groupMatcher.find()) {
            data.groupCode = groupMatcher.group(1).trim();
            logger.info("Found group code: {}", data.groupCode);
        }

        // Парсинг дисциплины
        Pattern discPattern = Pattern.compile("Дисциплина:\\s*([^\\n\\r]*)", Pattern.CASE_INSENSITIVE);
        Matcher discMatcher = discPattern.matcher(fullText);
        if (discMatcher.find()) {
            data.disciplineName = discMatcher.group(1).trim();
            logger.info("Found discipline: {}", data.disciplineName);
        }

        // Парсинг таблицы
        // 1. Пробуем формат полного отчета (4 колонки: Дисциплина, №, ФИО, Рейтинг)
        Pattern fullRowPattern = Pattern.compile("(\\S+)\\s+(\\d+)\\s+([^\\d\\n\\r]+?)\\s+(\\d+([.,]\\d+)?)");
        Matcher fullRowMatcher = fullRowPattern.matcher(fullText);
        boolean foundFull = false;
        
        while (fullRowMatcher.find()) {
            String disc = fullRowMatcher.group(1).trim();
            String name = fullRowMatcher.group(3).trim();
            String rating = fullRowMatcher.group(4).trim().replace(',', '.');
            
            if (!disc.equalsIgnoreCase("Дисциплина") && !name.equalsIgnoreCase("ФИО")) {
                // Если мы еще не определили дисциплину (для обычного отчета), берем первую попавшуюся
                if (data.disciplineName == null) {
                    data.disciplineName = disc;
                }
                // В текущей реализации мы импортируем только одну дисциплину за раз
                // Если это полный отчет, импортируем только ту дисциплину, которая совпала с первой найденной
                if (disc.equalsIgnoreCase(data.disciplineName)) {
                    data.students.add(new String[]{name, rating});
                    foundFull = true;
                }
            }
        }

        // 2. Если не нашли как полный отчет, пробуем как обычный (3 колонки: №, ФИО, Рейтинг)
        if (!foundFull) {
            Pattern rowPattern = Pattern.compile("(\\d+)\\s+([^\\d\\n\\r]+?)\\s+(\\d+([.,]\\d+)?)");
            Matcher rowMatcher = rowPattern.matcher(fullText);
            
            while (rowMatcher.find()) {
                String name = rowMatcher.group(2).trim();
                String rating = rowMatcher.group(3).trim().replace(',', '.');
                
                if (!name.equalsIgnoreCase("ФИО студента") && !name.equalsIgnoreCase("Рейтинг")) {
                    data.students.add(new String[]{name, rating});
                }
            }
        }

        if (data.groupCode == null) {
            throw new Exception("В PDF не найдена информация о группе (искали 'Группа: ...' или 'отчёт по группе ...')");
        }
        if (data.disciplineName == null) {
            throw new Exception("В PDF не найдена информация о дисциплине");
        }
        if (data.students.isEmpty()) {
            throw new Exception("В PDF не найдены данные о рейтингах студентов");
        }

        return data;
    }
}
