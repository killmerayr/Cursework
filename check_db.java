import java.sql.*;

public class check_db {
    public static void main(String[] args) {
        try {
            Class.forName("org.postgresql.Driver");
            String url = "jdbc:postgresql://localhost:5432/rating_system";
            String user = System.getenv("DB_USER");
            if (user == null) user = "rating_user";
            String password = System.getenv("DB_PASSWORD");
            if (password == null || password.isEmpty()) {
                System.err.println("Error: DB_PASSWORD environment variable is not set!");
                return;
            }
            
            Connection conn = DriverManager.getConnection(url, user, password);
            System.out.println("✓ Connected to PostgreSQL");
            
            // Check tables
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet tables = meta.getTables(null, "public", "%", new String[]{"TABLE"});
            System.out.println("\nTables in database:");
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                System.out.println("  - " + tableName);
                
                if (tableName.equals("ratings")) {
                    ResultSet columns = meta.getColumns(null, "public", tableName, null);
                    System.out.println("    Columns:");
                    while (columns.next()) {
                        System.out.println("      - " + columns.getString("COLUMN_NAME") + " (" + columns.getString("TYPE_NAME") + ")");
                    }
                    columns.close();
                }
            }
            tables.close();
            
            // Check data
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM ratings");
            if (rs.next()) {
                System.out.println("\nRatings count: " + rs.getInt(1));
            }
            rs.close();
            
            rs = stmt.executeQuery("SELECT * FROM ratings LIMIT 3");
            System.out.println("\nFirst 3 ratings:");
            while (rs.next()) {
                System.out.println("  ID=" + rs.getInt("id") + ", Student=" + rs.getInt("student_number") + 
                        ", Name=" + rs.getString("student_name") + ", Rating=" + rs.getDouble("rating"));
            }
            rs.close();
            stmt.close();
            
            conn.close();
            System.out.println("\n✓ Connection closed");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
