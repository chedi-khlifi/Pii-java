package example;

import com.mindforge.util.UserSession;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;

public class ExamController {

    /**
     * Returns the current logged-in user's ID from the MindForge session.
     * Falls back to 0 if no session is active (should never happen in normal flow).
     */
    private static int currentUserId() {
        return UserSession.getInstance().getUserId();
    }

    /**
     * Load only the exams that belong to the currently logged-in user.
     */
    public static ObservableList<Exam> getExams() {
        ObservableList<Exam> list = FXCollections.observableArrayList();
        String sql = "SELECT id, title, description, exam_date, duration_minutes, location, importance, owner_id " +
                     "FROM exams WHERE owner_id = ? ORDER BY exam_date ASC";
        try {
            Connection con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, currentUserId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new Exam(
                                rs.getInt("id"),
                                rs.getString("title"),
                                rs.getString("description") != null ? rs.getString("description") : "",
                                rs.getString("exam_date") != null ? rs.getString("exam_date") : "",
                                rs.getInt("duration_minutes"),
                                rs.getString("location") != null ? rs.getString("location") : "",
                                rs.getInt("importance"),
                                rs.getInt("owner_id")
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
     * Insert a new exam for the currently logged-in user.
     */
    public static void insertExam(String title, String description, String examDate,
                                  int durationMinutes, String location, int importance) {
        String sql = "INSERT INTO exams(title, description, exam_date, duration_minutes, " +
                     "location, importance, created_at, owner_id) VALUES (?, ?, ?, ?, ?, ?, NOW(), ?)";
        try {
            Connection con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, title);
                ps.setString(2, description);
                ps.setString(3, examDate);
                ps.setInt(4, durationMinutes);
                ps.setString(5, location);
                ps.setInt(6, importance);
                ps.setInt(7, currentUserId());
                ps.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateExam(int id, String title, String description, String examDate,
                                  int durationMinutes, String location, int importance) {
        // Only allow updating exams owned by the current user
        String sql = "UPDATE exams SET title=?, description=?, exam_date=?, duration_minutes=?, " +
                     "location=?, importance=? WHERE id=? AND owner_id=?";
        try {
            Connection con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, title);
                ps.setString(2, description);
                ps.setString(3, examDate);
                ps.setInt(4, durationMinutes);
                ps.setString(5, location);
                ps.setInt(6, importance);
                ps.setInt(7, id);
                ps.setInt(8, currentUserId());
                ps.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deleteExam(int id) {
        // Only allow deleting exams owned by the current user
        String sql = "DELETE FROM exams WHERE id=? AND owner_id=?";
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
