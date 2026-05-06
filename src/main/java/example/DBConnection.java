package example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static DBConnection instance;
    private Connection connection;

    private static final String URL      = "jdbc:mysql://127.0.0.1:3306/mindforge_db?useSSL=false";
    private static final String USER     = "root";
    private static final String PASSWORD = "";

    // Private constructor — prevents "new DBConnection()" from outside
    private DBConnection() throws SQLException {
        this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Returns the unique DBConnection instance (Singleton).
     * Re-creates the connection automatically if it was closed.
     */
    public static DBConnection getInstance() throws SQLException {
        if (instance == null || instance.connection.isClosed()) {
            instance = new DBConnection();
        }
        return instance;
    }

    /** Returns the underlying JDBC Connection. */
    public Connection getConnection() {
        return connection;
    }
}
