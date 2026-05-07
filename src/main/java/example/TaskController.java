package example;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;

public class TaskController {

    private static final int OWNER_ID = 4;

    public static ObservableList<Task> getTasks() {
        ObservableList<Task> list = FXCollections.observableArrayList();

        try {
            Connection con = DBConnection.getInstance().getConnection();
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT id, title, description, status, priority, due_date, owner_id, estimated_minutes FROM task")) {

                while (rs.next()) {
                    list.add(new Task(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("description") != null ? rs.getString("description") : "",
                            rs.getString("status"),
                            rs.getInt("priority"),
                            rs.getString("due_date") != null ? rs.getString("due_date") : "",
                            rs.getInt("owner_id"),
                            rs.getInt("estimated_minutes")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public static void insertTask(String title, String description, String status, int priority, String dueDate, int estimatedMinutes) {
        String sql = "INSERT INTO task(title, description, status, priority, due_date, estimated_minutes, created_at, owner_id) VALUES (?, ?, ?, ?, ?, ?, NOW(), ?)";

        try {
            Connection con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, title);
                ps.setString(2, description);
                ps.setString(3, status);
                ps.setInt(4, priority);
                if (dueDate == null || dueDate.isEmpty()) ps.setNull(5, Types.TIMESTAMP);
                else ps.setString(5, dueDate);
                ps.setInt(6, estimatedMinutes);
                ps.setInt(7, OWNER_ID);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateTask(int id, String title, String description, String status, int priority, String dueDate, int estimatedMinutes) {
        String sql = "UPDATE task SET title=?, description=?, status=?, priority=?, due_date=?, estimated_minutes=? WHERE id=?";

        try {
            Connection con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, title);
                ps.setString(2, description);
                ps.setString(3, status);
                ps.setInt(4, priority);
                if (dueDate == null || dueDate.isEmpty()) ps.setNull(5, Types.TIMESTAMP);
                else ps.setString(5, dueDate);
                ps.setInt(6, estimatedMinutes);
                ps.setInt(7, id);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ObservableList<Task> getTasksByOwner(int ownerId) {
        ObservableList<Task> list = FXCollections.observableArrayList();
        String sql = "SELECT id, title, description, status, priority, due_date, owner_id, estimated_minutes FROM task WHERE owner_id = ?";
        try {
            Connection con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, ownerId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new Task(
                                rs.getInt("id"),
                                rs.getString("title"),
                                rs.getString("description") != null ? rs.getString("description") : "",
                                rs.getString("status"),
                                rs.getInt("priority"),
                                rs.getString("due_date") != null ? rs.getString("due_date") : "",
                                rs.getInt("owner_id"),
                                rs.getInt("estimated_minutes")
                        ));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void deleteTask(int id) {
        String sql = "DELETE FROM task WHERE id=?";

        try {
            Connection con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
