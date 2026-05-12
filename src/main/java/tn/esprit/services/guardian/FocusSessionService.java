package tn.esprit.services.guardian;

import tn.esprit.Entity.Guardian.FocusSession;
import tn.esprit.db.DatabaseConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FocusSessionService {

    // ── Basic CRUD ────────────────────────────────────────────────────────────

    public List<FocusSession> findAll() throws SQLException {
        String sql = "SELECT id, duration, started_at, ended_at, session_type, user_id, task_id FROM focus_session";
        List<FocusSession> sessions = new ArrayList<>();
        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) sessions.add(mapRow(rs));
        }
        return sessions;
    }

    public FocusSession findById(int id) throws SQLException {
        String sql = "SELECT id, duration, started_at, ended_at, session_type, user_id, task_id FROM focus_session WHERE id = ?";
        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
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
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    public boolean update(FocusSession session) throws SQLException {
        if (session.id() == null) throw new IllegalArgumentException("FocusSession id must not be null for update.");
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

    // ── Symfony parity: FocusSessionRepository methods ───────────────────────

    /**
     * Returns the most recent sessions for a user, newest first.
     * Mirrors: findRecentByUser(User $user, int $limit = 10)
     */
    public List<FocusSession> findRecentByUser(int userId, int limit) throws SQLException {
        String sql = "SELECT id, duration, started_at, ended_at, session_type, user_id, task_id " +
                     "FROM focus_session WHERE user_id = ? ORDER BY started_at DESC LIMIT ?";
        List<FocusSession> sessions = new ArrayList<>();
        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) sessions.add(mapRow(rs));
            }
        }
        return sessions;
    }

    /**
     * Total focus minutes for a user across all time.
     * Mirrors: getTotalDurationByUser(User $user): int
     */
    public int getTotalDurationByUser(int userId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(duration), 0) FROM focus_session WHERE user_id = ?";
        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Number of sessions started today for a user.
     * Mirrors: getTodaySessionCountByUser(User $user): int
     */
    public int getTodaySessionCountByUser(int userId) throws SQLException {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime startOfTomorrow = startOfDay.plusDays(1);
        String sql = "SELECT COUNT(*) FROM focus_session WHERE user_id = ? " +
                     "AND started_at >= ? AND started_at < ?";
        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setTimestamp(2, Timestamp.valueOf(startOfDay));
            ps.setTimestamp(3, Timestamp.valueOf(startOfTomorrow));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Total focus minutes in the current week (Mon–Sun) for a user.
     * Mirrors: getWeekDurationByUser(User $user): int
     */
    public int getWeekDurationByUser(int userId) throws SQLException {
        // Monday of current week
        LocalDate today = LocalDate.now();
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDateTime weekStart = monday.atStartOfDay();
        LocalDateTime weekEnd   = weekStart.plusDays(7);
        String sql = "SELECT COALESCE(SUM(duration), 0) FROM focus_session WHERE user_id = ? " +
                     "AND started_at >= ? AND started_at < ?";
        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setTimestamp(2, Timestamp.valueOf(weekStart));
            ps.setTimestamp(3, Timestamp.valueOf(weekEnd));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Per-task total minutes for a user, sorted descending, limited to top N.
     * Returns list of maps with keys: task_id, task_title, total_minutes.
     * Mirrors: getPerTaskTotalsByUser(User $user, int $limit = 6): array
     */
    public List<Map<String, Object>> getPerTaskTotalsByUser(int userId, int limit) throws SQLException {
        String sql = "SELECT t.id AS task_id, t.title AS task_title, SUM(fs.duration) AS total_minutes " +
                     "FROM focus_session fs " +
                     "JOIN task t ON t.id = fs.task_id " +
                     "WHERE fs.user_id = ? " +
                     "GROUP BY t.id, t.title " +
                     "ORDER BY total_minutes DESC " +
                     "LIMIT ?";
        List<Map<String, Object>> results = new ArrayList<>();
        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new java.util.HashMap<>();
                    row.put("task_id",      rs.getInt("task_id"));
                    row.put("task_title",   rs.getString("task_title"));
                    row.put("total_minutes", rs.getInt("total_minutes"));
                    results.add(row);
                }
            }
        }
        return results;
    }

    /**
     * Checks for a duplicate session within the last windowSeconds seconds.
     * Prevents double-saves from rapid button clicks.
     * Mirrors: hasRecentDuplicate(User $user, Task $task, int $duration, int $windowSeconds = 20): bool
     */
    public boolean hasRecentDuplicate(int userId, int taskId, int duration, int windowSeconds) throws SQLException {
        LocalDateTime since = LocalDateTime.now().minusSeconds(Math.max(1, windowSeconds));
        String sql = "SELECT COUNT(*) FROM focus_session " +
                     "WHERE user_id = ? AND task_id = ? AND duration = ? AND started_at >= ?";
        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, taskId);
            ps.setInt(3, duration);
            ps.setTimestamp(4, Timestamp.valueOf(since));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    /**
     * Returns daily total minutes for a user between two dates.
     * All days in the range are initialised to 0 so charts stay continuous.
     * Mirrors: getDailyMinutesByUser(User $user, DateTimeImmutable $from, DateTimeImmutable $to): array
     *
     * @param userId  the user's ID
     * @param from    start date (inclusive)
     * @param to      end date (exclusive)
     * @return LinkedHashMap keyed by "yyyy-MM-dd", values are total minutes
     */
    public Map<String, Integer> getDailyMinutesByUser(int userId, LocalDate from, LocalDate to) throws SQLException {
        // Pre-fill every day with 0 to keep charts continuous
        Map<String, Integer> daily = new LinkedHashMap<>();
        LocalDate cursor = from;
        while (cursor.isBefore(to)) {
            daily.put(cursor.format(DateTimeFormatter.ISO_LOCAL_DATE), 0);
            cursor = cursor.plusDays(1);
        }

        String sql = "SELECT started_at, duration FROM focus_session " +
                     "WHERE user_id = ? AND started_at >= ? AND started_at < ? ORDER BY started_at ASC";
        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setTimestamp(2, Timestamp.valueOf(from.atStartOfDay()));
            ps.setTimestamp(3, Timestamp.valueOf(to.atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("started_at");
                    if (ts == null) continue;
                    String dayKey = ts.toLocalDateTime().toLocalDate()
                                     .format(DateTimeFormatter.ISO_LOCAL_DATE);
                    daily.merge(dayKey, rs.getInt("duration"), Integer::sum);
                }
            }
        }
        return daily;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
        if (value == null) ps.setNull(index, java.sql.Types.INTEGER);
        else               ps.setInt(index, value);
    }

    private static void setNullableTimestamp(PreparedStatement ps, int index, LocalDateTime value) throws SQLException {
        if (value == null) ps.setNull(index, java.sql.Types.TIMESTAMP);
        else               ps.setTimestamp(index, Timestamp.valueOf(value));
    }
}
