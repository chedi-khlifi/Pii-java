package example;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class ExamController {

    private static final int OWNER_ID = 1;

    public static ObservableList<Exam> getExams() {
        ObservableList<Exam> list = FXCollections.observableArrayList();

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, title, description, exam_date, duration_minutes, location, importance, owner_id FROM exams")) {

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

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public static void insertExam(String title, String description, String examDate, int durationMinutes, String location, int importance) {
        String sql = "INSERT INTO exams(title, description, exam_date, duration_minutes, location, importance, created_at, owner_id) VALUES (?, ?, ?, ?, ?, ?, NOW(), ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, title);
            ps.setString(2, description);
            ps.setString(3, examDate);
            ps.setInt(4, durationMinutes);
            ps.setString(5, location);
            ps.setInt(6, importance);
            ps.setInt(7, OWNER_ID);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateExam(int id, String title, String description, String examDate, int durationMinutes, String location, int importance) {
        String sql = "UPDATE exams SET title=?, description=?, exam_date=?, duration_minutes=?, location=?, importance=? WHERE id=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, title);
            ps.setString(2, description);
            ps.setString(3, examDate);
            ps.setInt(4, durationMinutes);
            ps.setString(5, location);
            ps.setInt(6, importance);
            ps.setInt(7, id);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deleteExam(int id) {
        String sql = "DELETE FROM exams WHERE id=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}