import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Step 1: Database Connectivity Test
 * Verifies that JDBC can connect to the existing MySQL database (ludo_db)
 * and read the record count from the existing 'games' table without modifying anything.
 */
public class DatabaseTest {

    public static void main(String[] args) {
        System.out.println("============================================");
        System.out.println("  Ludo Game - MySQL JDBC Connection Test    ");
        System.out.println("============================================");

        // 1. Load configuration using Config class
        Config.load();
        String host = Config.getDbHost();
        String port = Config.getDbPort();
        String dbName = Config.getDbName();
        String user = Config.getDbUser();
        String pass = Config.getDbPassword();

        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + dbName
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        System.out.println("[INFO] Target JDBC URL: " + jdbcUrl);
        System.out.println("[INFO] Connecting as user: " + user);

        // 2. Load the MySQL JDBC Driver
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("[INFO] MySQL JDBC Driver loaded successfully.");
        } catch (ClassNotFoundException e) {
            System.err.println("[ERROR] MySQL JDBC Driver not found in classpath!");
            System.err.println("       Make sure to include lib/mysql-connector-java-8.0.28.jar in classpath.");
            e.printStackTrace();
            System.exit(1);
        }

        // 3. Connect to MySQL and execute read-only test query
        String testSql = "SELECT COUNT(*) FROM games";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, pass);
             PreparedStatement stmt = conn.prepareStatement(testSql);
             ResultSet rs = stmt.executeQuery()) {

            System.out.println("[SUCCESS] Connected to database '" + dbName + "' successfully!");

            if (rs.next()) {
                int gameCount = rs.getInt(1);
                System.out.println("[SUCCESS] Query executed: " + testSql);
                System.out.println("[INFO] Current record count in 'games' table: " + gameCount);
            }

            System.out.println("============================================");
            System.out.println("  Result: JDBC CONNECTION TEST PASSED       ");
            System.out.println("============================================");

        } catch (SQLException e) {
            System.err.println("[ERROR] Database connection or query failed!");
            System.err.println("        SQLState: " + e.getSQLState());
            System.err.println("        ErrorCode: " + e.getErrorCode());
            System.err.println("        Message: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

