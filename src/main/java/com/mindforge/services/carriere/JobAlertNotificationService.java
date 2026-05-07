package com.mindforge.services.carriere;

import com.mindforge.entities.carriere.JobAlertNotification;
import com.mindforge.utils.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JobAlertNotificationService {

    private final Connection cnx;

    public JobAlertNotificationService() {
        try {
            cnx = DBConnection.getConnection();
            ensureTableExists();
        } catch (SQLException e) {
            throw new RuntimeException("DB connection error: " + e.getMessage(), e);
        }
    }

    private void ensureTableExists() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS job_alert_notification (
                id INT AUTO_INCREMENT PRIMARY KEY,
                subscription_id INT NOT NULL,
                opportunite_id INT NOT NULL,
                sent_at DATETIME NOT NULL,
                is_read TINYINT(1) NOT NULL DEFAULT 0,
                UNIQUE KEY uq_sub_opp (subscription_id, opportunite_id)
            )
            """;
        try (Statement st = cnx.createStatement()) {
            st.execute(sql);
            // Remove any bogus rows written before the generated-key bug was fixed
            st.execute("DELETE FROM job_alert_notification WHERE opportunite_id = 0");
        }
    }

    public void add(int subscriptionId, int opportuniteId) {
        String sql = "INSERT IGNORE INTO job_alert_notification (subscription_id, opportunite_id, sent_at, is_read) VALUES (?,?,?,0)";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, subscriptionId);
            pst.setInt(2, opportuniteId);
            pst.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            pst.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error adding notification: " + e.getMessage());
        }
    }

    public void markRead(int id) {
        String sql = "UPDATE job_alert_notification SET is_read=1 WHERE id=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error marking notification read: " + e.getMessage());
        }
    }

    public void markAllReadForUser(int userId) {
        String sql = """
            UPDATE job_alert_notification n
            JOIN job_alert_subscription s ON n.subscription_id = s.id
            SET n.is_read = 1
            WHERE s.user_id = ?
            """;
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, userId);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error marking all read: " + e.getMessage());
        }
    }

    public List<JobAlertNotification> getByUserId(int userId) {
        List<JobAlertNotification> list = new ArrayList<>();
        String sql = """
            SELECT n.*, o.title AS opp_title, o.type AS opp_type,
                   o.location AS opp_location, e.name AS company_name,
                   s.keywords AS sub_keywords
            FROM job_alert_notification n
            JOIN job_alert_subscription s ON n.subscription_id = s.id
            JOIN opportunite_carriere o ON n.opportunite_id = o.id
            LEFT JOIN entreprise e ON o.company_id = e.id
            WHERE s.user_id = ?
            ORDER BY n.sent_at DESC
            """;
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.out.println("Error fetching notifications: " + e.getMessage());
        }
        return list;
    }

    public int countUnreadForUser(int userId) {
        String sql = """
            SELECT COUNT(*) FROM job_alert_notification n
            JOIN job_alert_subscription s ON n.subscription_id = s.id
            WHERE s.user_id = ? AND n.is_read = 0
            """;
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.out.println("Error counting unread: " + e.getMessage());
        }
        return 0;
    }

    private JobAlertNotification mapRow(ResultSet rs) throws SQLException {
        JobAlertNotification n = new JobAlertNotification();
        n.setId(rs.getInt("id"));
        n.setSubscriptionId(rs.getInt("subscription_id"));
        n.setOpportuniteId(rs.getInt("opportunite_id"));
        Timestamp ts = rs.getTimestamp("sent_at");
        if (ts != null) n.setSentAt(ts.toLocalDateTime());
        n.setRead(rs.getBoolean("is_read"));
        n.setOpportunityTitle(rs.getString("opp_title"));
        n.setOpportunityType(rs.getString("opp_type"));
        n.setOpportunityLocation(rs.getString("opp_location"));
        n.setCompanyName(rs.getString("company_name"));
        n.setSubscriptionKeywords(rs.getString("sub_keywords"));
        return n;
    }
}
