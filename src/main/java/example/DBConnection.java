package org.example;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {
    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(
                "jdbc:mysql://127.0.0.1:3306/mindforge_db?useSSL=false",
                "root",
                ""
        );
    }
}