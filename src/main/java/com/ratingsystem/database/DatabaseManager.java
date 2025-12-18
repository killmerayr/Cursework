package com.ratingsystem.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.sql.*;
import java.util.Properties;
import java.io.File;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;

/**
 * Менеджер для работы с БД (SQLite или PostgreSQL)
 * Singleton паттерн
 */
public class DatabaseManager {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static DatabaseManager instance;
    private Connection connection;
    private String dbType;
    private String dbUrl;

    private DatabaseManager() {
    }

    /**
     * Получить единственный экземпляр DatabaseManager
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Загрузить конфигурацию из application.properties
     * Сначала пробует системную папку (только для админа), потом JAR
     */
    private Properties loadConfiguration() {
        Properties props = new Properties();
        
        // Определить путь к системной папке
        String systemPath = getSystemConfigPath();
        File systemConfigFile = new File(systemPath);
        
        // Попытка 1: Читать из системной папки (/etc/rating-system или C:\ProgramData\RatingSystem)
        if (systemConfigFile.exists()) {
            try (InputStream input = new FileInputStream(systemConfigFile)) {
                props.load(input);
                logger.info("Configuration loaded from system path: {}", systemPath);
                return props;
            } catch (IOException e) {
                logger.error("Failed to read configuration from system path: {}", systemPath, e);
                throw new RuntimeException("Cannot access database configuration at: " + systemPath, e);
            }
        }
        
        // Попытка 2: Читать из JAR (только для development)
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                logger.error("Configuration not found in system path: {} and not found in JAR", systemPath);
                throw new RuntimeException("Database configuration not found! Please create config at: " + systemPath);
            }
            props.load(input);
            logger.warn("Configuration loaded from JAR (development mode). For production, use: {}", systemPath);
            return props;
        } catch (IOException e) {
            logger.error("Error loading configuration from JAR", e);
            throw new RuntimeException("Failed to load configuration", e);
        }
    }
    
    /**
     * Получить путь к системному конфиг файлу в зависимости от ОС
     */
    private String getSystemConfigPath() {
        String osName = System.getProperty("os.name").toLowerCase();
        
        if (osName.contains("win")) {
            // Windows: C:\ProgramData\RatingSystem\application.properties
            return "C:\\ProgramData\\RatingSystem\\application.properties";
        } else if (osName.contains("mac") || osName.contains("linux")) {
            // Linux/macOS: /etc/rating-system/application.properties
            return "/etc/rating-system/application.properties";
        } else {
            // Fallback
            return "/etc/rating-system/application.properties";
        }
    }

    /**
     * Инициализировать БД и создать таблицы
     * ВНИМАНИЕ: Используется ТОЛЬКО PostgreSQL
     */
    public void initialize() {
        try {
            Properties config = loadConfiguration();
            logger.info("Initializing PostgreSQL database connection");
            
            initializePostgreSQL(config);
            createTables();
            logger.info("Database tables created/verified");
        } catch (Exception e) {
            logger.error("Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * Инициализировать PostgreSQL подключение
     */
    private void initializePostgreSQL(Properties config) throws ClassNotFoundException, SQLException {
        // Приоритет переменным окружения (для Docker), затем конфиг файлу
        String host = System.getenv("DB_HOST");
        if (host == null) host = config.getProperty("db.postgresql.host", "localhost");
        
        String port = System.getenv("DB_PORT");
        if (port == null) port = config.getProperty("db.postgresql.port", "5432");
        
        String database = System.getenv("DB_NAME");
        if (database == null) database = config.getProperty("db.postgresql.database", "rating_system");
        
        String user = System.getenv("DB_USER");
        if (user == null) user = config.getProperty("db.postgresql.user", "postgres");
        
        String password = System.getenv("DB_PASSWORD");
        if (password == null || password.trim().isEmpty()) {
            password = config.getProperty("db.postgresql.password");
        }
        
        if (password == null || password.trim().isEmpty() || password.equals("${DB_PASSWORD}")) {
            logger.error("Database password not found! Set DB_PASSWORD environment variable.");
            throw new SQLException("Database password is not configured. Security requirement not met.");
        }
        
        dbUrl = String.format("jdbc:postgresql://%s:%s/%s?sslmode=disable", host, port, database);
        
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            logger.error("PostgreSQL JDBC driver not found");
            throw e;
        }
        
        connection = DriverManager.getConnection(dbUrl, user, password);
        connection.setAutoCommit(true);
        logger.info("✓ Connected to PostgreSQL: {}:{}/{}", host, port, database);
    }

    /**
     * Создать таблицы в БД
     */
    private void createTables() throws SQLException {
        String[] tables = getPostgreSQLTables();

        try (Statement stmt = connection.createStatement()) {
            for (String table : tables) {
                stmt.execute(table);
            }
        }
        
        // Запустить миграции для обновления схемы
        runMigrations();
        logger.info("Tables created successfully");
    }

    /**
     * Запустить миграции БД
     */
    private void runMigrations() throws SQLException {
        // Проверить и добавить поле student_name в таблицу ratings если его нет
        try (Statement stmt = connection.createStatement()) {
            try {
                // Попытаться выбрать из колонки student_name
                stmt.executeQuery("SELECT student_name FROM ratings LIMIT 1");
                logger.info("Column student_name already exists in ratings table");
            } catch (SQLException e) {
                if (e.getMessage().contains("column \"student_name\" does not exist")) {
                    // Добавить колонку если её нет
                    logger.info("Adding student_name column to ratings table");
                    stmt.execute("ALTER TABLE ratings ADD COLUMN student_name VARCHAR(255)");
                    logger.info("Column student_name added successfully");
                } else {
                    throw e;
                }
            }
        }
    }

    /**
     * SQL для PostgreSQL
     */
    private String[] getPostgreSQLTables() {
        return new String[]{
            // Таблица пользователей
            "CREATE TABLE IF NOT EXISTS users (" +
            "  id SERIAL PRIMARY KEY," +
            "  username VARCHAR(255) UNIQUE NOT NULL," +
            "  password_hash VARCHAR(255) NOT NULL," +
            "  role VARCHAR(50) NOT NULL," +
            "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ");",

            // Таблица групп
            "CREATE TABLE IF NOT EXISTS groups (" +
            "  id SERIAL PRIMARY KEY," +
            "  group_code VARCHAR(255) UNIQUE NOT NULL," +
            "  student_count INTEGER NOT NULL," +
            "  discipline_count INTEGER NOT NULL," +
            "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ");",

            // Таблица дисциплин
            "CREATE TABLE IF NOT EXISTS disciplines (" +
            "  id SERIAL PRIMARY KEY," +
            "  group_id INTEGER NOT NULL," +
            "  discipline_code VARCHAR(255) NOT NULL," +
            "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "  FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE," +
            "  UNIQUE(group_id, discipline_code)" +
            ");",

            // Таблица рейтингов студентов
            "CREATE TABLE IF NOT EXISTS ratings (" +
            "  id SERIAL PRIMARY KEY," +
            "  discipline_id INTEGER NOT NULL," +
            "  student_number INTEGER NOT NULL," +
            "  student_name VARCHAR(255)," +
            "  rating REAL NOT NULL CHECK(rating >= 0 AND rating <= 100)," +
            "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "  FOREIGN KEY (discipline_id) REFERENCES disciplines(id) ON DELETE CASCADE," +
            "  UNIQUE(discipline_id, student_number)" +
            ");",

            // Таблица сводок
            "CREATE TABLE IF NOT EXISTS summaries (" +
            "  id SERIAL PRIMARY KEY," +
            "  group_id INTEGER NOT NULL," +
            "  discipline_id INTEGER NOT NULL," +
            "  avg_rating REAL NOT NULL," +
            "  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "  FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE," +
            "  FOREIGN KEY (discipline_id) REFERENCES disciplines(id) ON DELETE CASCADE," +
            "  UNIQUE(group_id, discipline_id)" +
            ");"
        };
    }

    /**
     * Получить соединение с БД (с проверкой на валидность)
     */
    public Connection getConnection() throws SQLException {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                logger.info("Database connection is closed or invalid. Reconnecting...");
                Properties config = loadConfiguration();
                initializePostgreSQL(config);
            }
        } catch (Exception e) {
            logger.error("Failed to reconnect to database", e);
            throw new SQLException("Database connection lost and could not be restored", e);
        }
        return connection;
    }

    /**
     * Выполнить SQL запрос (SELECT) - Возвращает CachedRowSet, который не требует открытого соединения
     */
    public ResultSet executeQuery(String sql) throws SQLException {
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            RowSetFactory factory = RowSetProvider.newFactory();
            CachedRowSet crs = factory.createCachedRowSet();
            crs.populate(rs);
            return crs;
        }
    }

    /**
     * Выполнить SQL обновление (UPDATE, INSERT, DELETE)
     * Автоматически закрывает statement
     */
    public int executeUpdate(String sql) throws SQLException {
        try (Statement stmt = getConnection().createStatement()) {
            return stmt.executeUpdate(sql);
        }
    }

    /**
     * Выполнить SQL обновление с подготовленным оператором
     * Автоматически закрывает statement
     */
    public int executeUpdate(String sql, Object... params) throws SQLException {
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            return pstmt.executeUpdate();
        }
    }

    /**
     * Выполнить SQL запрос с подготовленным оператором - Возвращает CachedRowSet
     */
    public ResultSet executeQuery(String sql, Object... params) throws SQLException {
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                RowSetFactory factory = RowSetProvider.newFactory();
                CachedRowSet crs = factory.createCachedRowSet();
                crs.populate(rs);
                return crs;
            }
        }
    }

    /**
     * Закрыть соединение с БД
     */
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                logger.info("Database connection closed");
            } catch (SQLException e) {
                logger.error("Error closing database connection", e);
            }
        }
    }
}
