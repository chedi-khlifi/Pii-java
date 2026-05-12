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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AiInsightService {

    // ── Basic CRUD ────────────────────────────────────────────────────────────

    public List<AiInsight> findAll() throws SQLException {
        String sql = "SELECT id, user_id, task_id, type, source, payload, created_at " +
                     "FROM guardian_ai_insight ORDER BY created_at DESC";
        List<AiInsight> insights = new ArrayList<>();
        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) insights.add(mapRow(rs));
        }
        return insights;
    }

    public AiInsight findById(int id) throws SQLException {
        String sql = "SELECT id, user_id, task_id, type, source, payload, created_at " +
                     "FROM guardian_ai_insight WHERE id = ?";
        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    /**
     * Filters insights by type and/or source.
     * Mirrors: createAdminFilteredQuery(?string $type, ?string $source)
     */
    public List<AiInsight> findFiltered(String type, String source) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT id, user_id, task_id, type, source, payload, created_at " +
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
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) insights.add(mapRow(rs));
            }
        }
        return insights;
    }

    public int insert(AiInsight insight) throws SQLException {
        String sql = "INSERT INTO guardian_ai_insight (user_id, task_id, type, source, payload, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, insight.userId());
            setNullableInt(ps, 2, insight.taskId());
            ps.setString(3, insight.type());
            ps.setString(4, insight.source());
            ps.setString(5, insight.payload());
            ps.setTimestamp(6, Timestamp.valueOf(
                    insight.createdAt() == null ? LocalDateTime.now() : insight.createdAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    // ── Symfony parity: AiInsightRepository methods ──────────────────────────

    /**
     * Returns a count of insights grouped by source ("ai" vs "rule").
     * Mirrors: getSourceDistribution(?string $type, ?string $source): array
     *
     * @return map of source → count, e.g. {"ai": 12, "rule": 5}
     */
    public Map<String, Integer> getSourceDistribution(String type, String source) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT source, COUNT(*) AS total FROM guardian_ai_insight WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (type != null && !type.isBlank()) {
            sql.append(" AND type = ?");
            params.add(type.trim());
        }
        if (source != null && !source.isBlank()) {
            sql.append(" AND source = ?");
            params.add(source.trim());
        }
        sql.append(" GROUP BY source ORDER BY total DESC");

        Map<String, Integer> distribution = new LinkedHashMap<>();
        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) distribution.put(rs.getString("source"), rs.getInt("total"));
            }
        }
        return distribution;
    }

    /**
     * Returns a count of insights grouped by type.
     * Mirrors: getTypeDistribution(?string $type, ?string $source): array
     *
     * @return map of type → count, e.g. {"focus_tips": 8, "daily_plan": 4}
     */
    public Map<String, Integer> getTypeDistribution(String type, String source) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT type, COUNT(*) AS total FROM guardian_ai_insight WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (type != null && !type.isBlank()) {
            sql.append(" AND type = ?");
            params.add(type.trim());
        }
        if (source != null && !source.isBlank()) {
            sql.append(" AND source = ?");
            params.add(source.trim());
        }
        sql.append(" GROUP BY type ORDER BY total DESC");

        Map<String, Integer> distribution = new LinkedHashMap<>();
        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) distribution.put(rs.getString("type"), rs.getInt("total"));
            }
        }
        return distribution;
    }

    /**
     * Increments the helpful_votes counter for an insight.
     * Mirrors: incrementHelpfulVotes() + flush() in focusTimerApiAiFeedback().
     */
    public void incrementHelpfulVotes(int insightId) throws SQLException {
        updateVoteColumn(insightId, "helpful_votes");
    }

    /**
     * Increments the unhelpful_votes counter for an insight.
     * Mirrors: incrementUnhelpfulVotes() + flush() in focusTimerApiAiFeedback().
     */
    public void incrementUnhelpfulVotes(int insightId) throws SQLException {
        updateVoteColumn(insightId, "unhelpful_votes");
    }

    private void updateVoteColumn(int insightId, String column) throws SQLException {
        // Column name is controlled internally — no SQL injection risk
        String sql = "UPDATE guardian_ai_insight SET " + column + " = " + column + " + 1 WHERE id = ?";
        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, insightId);
            ps.executeUpdate();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AiInsight mapRow(ResultSet rs) throws SQLException {
        return new AiInsight(
                rs.getInt("id"),
                rs.getInt("user_id"),
                getNullableInt(rs, "task_id"),
                rs.getString("type"),
                rs.getString("source"),
                rs.getString("payload"),
                safeInt(rs, "helpful_votes"),
                safeInt(rs, "unhelpful_votes"),
                toLocalDateTime(rs.getTimestamp("created_at"))
        );
    }

    /** Reads an int column that may not exist in older schema versions. */
    private static Integer safeInt(ResultSet rs, String column) {
        try {
            int v = rs.getInt(column);
            return rs.wasNull() ? 0 : v;
        } catch (SQLException e) {
            return 0; // column doesn't exist yet — return 0 gracefully
        }
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static Integer getNullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) ps.setNull(index, java.sql.Types.INTEGER);
        else               ps.setInt(index, value);
    }
}
