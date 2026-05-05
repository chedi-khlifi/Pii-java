package tn.esprit.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        String url = System.getProperty("db.url",
                "jdbc:mysql://127.0.0.1:3306/mindforge_db?useSSL=false");
        String user = System.getProperty("db.user", "root");
        String password = System.getProperty("db.password", "");
        return DriverManager.getConnection(url, user, password);
    }
}
