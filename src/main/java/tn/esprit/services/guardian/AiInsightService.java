package tn.esprit.services.guardian;

import tn.esprit.Entity.Guardian.AiInsight;
import tn.esprit.db.DatabaseConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AiInsightService {

    public List<AiInsight> findAll() throws SQLException {
        String sql = "SELECT id, user_id, task_id, type, source, payload, helpful_votes, unhelpful_votes, created_at " +
                "FROM guardian_ai_insight ORDER BY created_at DESC";
        List<AiInsight> insights = new ArrayList<>();

        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                insights.add(mapRow(rs));
            }
        }

        return insights;
    }

    public List<AiInsight> findFiltered(String type, String source) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT id, user_id, task_id, type, source, payload, helpful_votes, unhelpful_votes, created_at " +
                        "FROM guardian_ai_insight WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (type != null && !type.isBlank()) {
            sql.append(" AND type = ?");
            params.add(type.trim());
        }
        if (source != null && !source.isBlank()) {
            sql.append(" AND source = ?");
            params.add(source.trim());
        }

        sql.append(" ORDER BY created_at DESC");

        List<AiInsight> insights = new ArrayList<>();
        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    insights.add(mapRow(rs));
                }
            }
        }

        return insights;
    }

    public int insert(AiInsight insight) throws SQLException {
        String sql = "INSERT INTO guardian_ai_insight (user_id, task_id, type, source, payload, helpful_votes, unhelpful_votes, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, insight.userId());
            setNullableInt(ps, 2, insight.taskId());
            ps.setString(3, insight.type());
            ps.setString(4, insight.source());
            ps.setString(5, insight.payload());
            ps.setInt(6, insight.helpfulVotes() == null ? 0 : insight.helpfulVotes());
            ps.setInt(7, insight.unhelpfulVotes() == null ? 0 : insight.unhelpfulVotes());
            ps.setTimestamp(8, Timestamp.valueOf(insight.createdAt() == null ? LocalDateTime.now() : insight.createdAt()));

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }

        return -1;
    }

    private AiInsight mapRow(ResultSet rs) throws SQLException {
        return new AiInsight(
                rs.getInt("id"),
                rs.getInt("user_id"),
                getNullableInt(rs, "task_id"),
                rs.getString("type"),
                rs.getString("source"),
                rs.getString("payload"),
                rs.getInt("helpful_votes"),
                rs.getInt("unhelpful_votes"),
                toLocalDateTime(rs.getTimestamp("created_at"))
        );
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static Integer getNullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }
}
