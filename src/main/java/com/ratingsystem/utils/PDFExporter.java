package com.ratingsystem.utils;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import com.itextpdf.text.pdf.BaseFont;

/**
 * Экспортер данных в PDF формат
 */
public class PDFExporter {

    private static final Logger logger = LoggerFactory.getLogger(PDFExporter.class);
    
    private static Font getFont(float size, int style) {
        try {
            // Попытка найти шрифт с поддержкой кириллицы
            String[] fontPaths = {
                "/Library/Fonts/Arial Unicode.ttf",
                "/System/Library/Fonts/Supplemental/Arial.ttf",
                "C:\\Windows\\Fonts\\arial.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
            };
            
            String selectedFont = null;
            for (String path : fontPaths) {
                if (new File(path).exists()) {
                    selectedFont = path;
                    break;
                }
            }
            
            if (selectedFont != null) {
                BaseFont bf = BaseFont.createFont(selectedFont, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                return new Font(bf, size, style);
            }
        } catch (Exception e) {
            logger.warn("Could not load Cyrillic font, falling back to default", e);
        }
        return new Font(Font.FontFamily.HELVETICA, size, style);
    }

    private static final Font TITLE_FONT = getFont(16, Font.BOLD);
    private static final Font HEADER_FONT = getFont(12, Font.BOLD);
    private static final Font NORMAL_FONT = getFont(10, Font.NORMAL);
    private static final Font SMALL_FONT = getFont(9, Font.NORMAL);

    /**
     * Экспортировать сводку рейтингов в PDF
     */
    public static void exportSummaryToPDF(String filename, String groupCode, 
                                          Map<String, Double> disciplineRatings) {
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(filename));
            document.open();

            // Заголовок
            Paragraph title = new Paragraph("Сводка рейтингов по группе", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            // Информация о группе
            document.add(new Paragraph(" "));
            Paragraph groupInfo = new Paragraph("Группа: " + groupCode, HEADER_FONT);
            document.add(groupInfo);

            Paragraph dateInfo = new Paragraph("Дата формирования: " + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")), NORMAL_FONT);
            document.add(dateInfo);

            document.add(new Paragraph(" "));

            // Таблица с рейтингами
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);

            // Заголовки таблицы
            PdfPCell headerCell1 = new PdfPCell(new Phrase("Код дисциплины", HEADER_FONT));
            headerCell1.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table.addCell(headerCell1);

            PdfPCell headerCell2 = new PdfPCell(new Phrase("Общий рейтинг", HEADER_FONT));
            headerCell2.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table.addCell(headerCell2);

            // Заполнение таблицы данными
            for (Map.Entry<String, Double> entry : disciplineRatings.entrySet()) {
                table.addCell(new PdfPCell(new Phrase(entry.getKey(), NORMAL_FONT)));
                table.addCell(new PdfPCell(new Phrase(
                        String.format("%.2f", entry.getValue()), NORMAL_FONT)));
            }

            document.add(table);

            document.close();
            logger.info("PDF exported successfully to: {}", filename);
        } catch (Exception e) {
            logger.error("Error exporting to PDF", e);
            throw new RuntimeException("PDF export failed", e);
        }
    }

    /**
     * Экспортировать полный отчёт в PDF
     */
    public static void exportFullReportToPDF(String filename, String title, 
                                              String[] headers, String[][] data) {
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(filename));
            document.open();

            // Заголовок
            Paragraph titlePara = new Paragraph(title, TITLE_FONT);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            document.add(titlePara);

            document.add(new Paragraph(" "));

            // Таблица
            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);

            // Заголовки таблицы
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, HEADER_FONT));
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                table.addCell(cell);
            }

            // Данные
            for (String[] row : data) {
                for (String cell : row) {
                    table.addCell(new PdfPCell(new Phrase(cell, NORMAL_FONT)));
                }
            }

            document.add(table);
            document.close();
            logger.info("Full report exported successfully to: {}", filename);
        } catch (Exception e) {
            logger.error("Error exporting full report to PDF", e);
            throw new RuntimeException("PDF export failed", e);
        }
    }

    /**
     * Экспортировать рейтинги по дисциплине с ФИО студентов
     */
    public static void exportDisciplineRatingsToPDF(String filename, String groupCode, 
                                                     String disciplineName, List<String[]> studentsData) {
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(filename));
            document.open();

            // Заголовок
            Paragraph title = new Paragraph("Рейтинги студентов по дисциплине", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph(" "));
            Paragraph groupInfo = new Paragraph("Группа: " + groupCode, HEADER_FONT);
            document.add(groupInfo);

            Paragraph discInfo = new Paragraph("Дисциплина: " + disciplineName, HEADER_FONT);
            document.add(discInfo);

            Paragraph dateInfo = new Paragraph("Дата формирования: " + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")), SMALL_FONT);
            document.add(dateInfo);

            document.add(new Paragraph(" "));

            // Таблица с рейтингами
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1, 2, 1.5f});

            // Заголовки таблицы
            String[] headers = {"№ п/п", "ФИО студента", "Рейтинг"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, HEADER_FONT));
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                cell.setPadding(5);
                table.addCell(cell);
            }

            // Данные студентов
            for (int i = 0; i < studentsData.size(); i++) {
                String[] row = studentsData.get(i);
                PdfPCell numCell = new PdfPCell(new Phrase((i + 1) + "", NORMAL_FONT));
                numCell.setPadding(4);
                table.addCell(numCell);
                
                PdfPCell nameCell = new PdfPCell(new Phrase(row[0], NORMAL_FONT));
                nameCell.setPadding(4);
                table.addCell(nameCell);
                
                PdfPCell ratingCell = new PdfPCell(new Phrase(row[1], NORMAL_FONT));
                ratingCell.setPadding(4);
                ratingCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(ratingCell);
            }

            document.add(table);
            document.close();
            logger.info("Discipline ratings PDF exported successfully to: {}", filename);
        } catch (Exception e) {
            logger.error("Error exporting discipline ratings to PDF", e);
            throw new RuntimeException("PDF export failed", e);
        }
    }
}
