package tn.esprit.services.guardian;

import tn.esprit.Entity.Guardian.FocusSession;
import tn.esprit.db.DatabaseConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FocusSessionService {

    public List<FocusSession> findAll() throws SQLException {
        String sql = "SELECT id, duration, started_at, ended_at, session_type, user_id, task_id FROM focus_session";
        List<FocusSession> sessions = new ArrayList<>();

        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                sessions.add(mapRow(rs));
            }
        }

        return sessions;
    }

    public FocusSession findById(int id) throws SQLException {
        String sql = "SELECT id, duration, started_at, ended_at, session_type, user_id, task_id FROM focus_session WHERE id = ?";

        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }

        return null;
    }

    public int insert(FocusSession session) throws SQLException {
        String sql = "INSERT INTO focus_session (duration, started_at, ended_at, session_type, user_id, task_id) VALUES (?, ?, ?, ?, ?, ?)";

        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, session.duration());
            ps.setTimestamp(2, Timestamp.valueOf(session.startedAt()));
            setNullableTimestamp(ps, 3, session.endedAt());
            ps.setString(4, session.sessionType());
            ps.setInt(5, session.userId());
            setNullableInt(ps, 6, session.taskId());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }

        return -1;
    }

    public boolean update(FocusSession session) throws SQLException {
        if (session.id() == null) {
            throw new IllegalArgumentException("FocusSession id must not be null for update.");
        }

        String sql = "UPDATE focus_session SET duration = ?, started_at = ?, ended_at = ?, session_type = ?, user_id = ?, task_id = ? WHERE id = ?";

        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, session.duration());
            ps.setTimestamp(2, Timestamp.valueOf(session.startedAt()));
            setNullableTimestamp(ps, 3, session.endedAt());
            ps.setString(4, session.sessionType());
            ps.setInt(5, session.userId());
            setNullableInt(ps, 6, session.taskId());
            ps.setInt(7, session.id());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM focus_session WHERE id = ?";

        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private FocusSession mapRow(ResultSet rs) throws SQLException {
        return new FocusSession(
                rs.getInt("id"),
                rs.getInt("duration"),
                toLocalDateTime(rs.getTimestamp("started_at")),
                toLocalDateTime(rs.getTimestamp("ended_at")),
                rs.getString("session_type"),
                rs.getInt("user_id"),
                getNullableInt(rs, "task_id")
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

    private static void setNullableTimestamp(PreparedStatement ps, int index, LocalDateTime value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.TIMESTAMP);
        } else {
            ps.setTimestamp(index, Timestamp.valueOf(value));
        }
    }
}
