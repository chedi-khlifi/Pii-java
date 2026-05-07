package com.mindforge.services.carriere;

import com.mindforge.entities.carriere.JobAlertSubscription;
import com.mindforge.utils.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JobAlertSubscriptionService {

    private final Connection cnx;

    public JobAlertSubscriptionService() {
        try {
            cnx = DBConnection.getConnection();
            ensureTableExists();
        } catch (SQLException e) {
            throw new RuntimeException("DB connection error: " + e.getMessage(), e);
        }
    }

    private void ensureTableExists() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS job_alert_subscription (
                id INT AUTO_INCREMENT PRIMARY KEY,
                user_id INT NOT NULL,
                keywords VARCHAR(255),
                type VARCHAR(50),
                location VARCHAR(255),
                is_active TINYINT(1) NOT NULL DEFAULT 1,
                created_at DATETIME NOT NULL
            )
            """;
        try (Statement st = cnx.createStatement()) {
            st.execute(sql);
        }
    }

    public void add(JobAlertSubscription s) {
        String sql = "INSERT INTO job_alert_subscription (user_id, keywords, type, location, is_active, created_at) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, s.getUserId());
            pst.setString(2, s.getKeywords());
            pst.setString(3, s.getType());
            pst.setString(4, s.getLocation());
            pst.setBoolean(5, s.isActive());
            pst.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            pst.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error adding subscription: " + e.getMessage());
        }
    }

    public void update(JobAlertSubscription s) {
        String sql = "UPDATE job_alert_subscription SET keywords=?, type=?, location=?, is_active=? WHERE id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setString(1, s.getKeywords());
            pst.setString(2, s.getType());
            pst.setString(3, s.getLocation());
            pst.setBoolean(4, s.isActive());
            pst.setInt(5, s.getId());
            pst.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error updating subscription: " + e.getMessage());
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM job_alert_subscription WHERE id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error deleting subscription: " + e.getMessage());
        }
    }

    public void toggle(int id) {
        String sql = "UPDATE job_alert_subscription SET is_active = NOT is_active WHERE id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error toggling subscription: " + e.getMessage());
        }
    }

    public List<JobAlertSubscription> getByUserId(int userId) {
        List<JobAlertSubscription> list = new ArrayList<>();
        String sql = "SELECT * FROM job_alert_subscription WHERE user_id=? ORDER BY created_at DESC";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.out.println("Error fetching subscriptions: " + e.getMessage());
        }
        return list;
    }

    public List<JobAlertSubscription> getAllActive() {
        List<JobAlertSubscription> list = new ArrayList<>();
        String sql = "SELECT * FROM job_alert_subscription WHERE is_active=1";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.out.println("Error fetching active subscriptions: " + e.getMessage());
        }
        return list;
    }

    private JobAlertSubscription mapRow(ResultSet rs) throws SQLException {
        JobAlertSubscription s = new JobAlertSubscription();
        s.setId(rs.getInt("id"));
        s.setUserId(rs.getInt("user_id"));
        s.setKeywords(rs.getString("keywords"));
        s.setType(rs.getString("type"));
        s.setLocation(rs.getString("location"));
        s.setActive(rs.getBoolean("is_active"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) s.setCreatedAt(ts.toLocalDateTime());
        return s;
    }
}
