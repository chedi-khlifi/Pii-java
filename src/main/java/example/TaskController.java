package example;

import com.mindforge.util.UserSession;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;

public class TaskController {

    /**
     * Returns the current logged-in user's ID from the MindForge session.
     * Falls back to 0 if no session is active (should never happen in normal flow).
     */
    private static int currentUserId() {
        return UserSession.getInstance().getUserId();
    }

    /**
     * Load only the tasks that belong to the currently logged-in user.
     */
    public static ObservableList<Task> getTasks() {
        return getTasksByOwner(currentUserId());
    }

    public static ObservableList<Task> getTasksByOwner(int ownerId) {
        ObservableList<Task> list = FXCollections.observableArrayList();
        String sql = "SELECT id, title, description, status, priority, due_date, owner_id, estimated_minutes " +
                     "FROM task WHERE owner_id = ? ORDER BY due_date ASC";
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

    /**
     * Insert a new task for the currently logged-in user.
     */
    public static void insertTask(String title, String description, String status,
                                  int priority, String dueDate, int estimatedMinutes) {
        String sql = "INSERT INTO task(title, description, status, priority, due_date, " +
                     "estimated_minutes, created_at, owner_id) VALUES (?, ?, ?, ?, ?, ?, NOW(), ?)";
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
                ps.setInt(7, currentUserId());
                ps.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateTask(int id, String title, String description, String status,
                                  int priority, String dueDate, int estimatedMinutes) {
        String sql = "UPDATE task SET title=?, description=?, status=?, priority=?, " +
                     "due_date=?, estimated_minutes=? WHERE id=? AND owner_id=?";
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
                ps.setInt(8, currentUserId());
                ps.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deleteTask(int id) {
        // Only allow deleting tasks owned by the current user
        String sql = "DELETE FROM task WHERE id=? AND owner_id=?";
        try {
            Connection con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.setInt(2, currentUserId());
                ps.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
