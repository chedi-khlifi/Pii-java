package tn.esprit.services.guardian;

import tn.esprit.Entity.Guardian.VirtualRoom;
import tn.esprit.db.DatabaseConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VirtualRoomService {

    // ── Basic CRUD ────────────────────────────────────────────────────────────

    public List<VirtualRoom> findAll() throws SQLException {
        String sql = "SELECT id, name, description, is_active, max_participants, created_at, creator_id, subject_id FROM virtual_room";
        List<VirtualRoom> rooms = new ArrayList<>();
        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) rooms.add(mapRow(rs));
        }
        return rooms;
    }

    public List<VirtualRoom> findByCreator(int creatorId) throws SQLException {
        String sql = "SELECT id, name, description, is_active, max_participants, created_at, creator_id, subject_id FROM virtual_room WHERE creator_id = ?";
        List<VirtualRoom> rooms = new ArrayList<>();
        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, creatorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rooms.add(mapRow(rs));
            }
        }
        return rooms;
    }

    public VirtualRoom findById(int id) throws SQLException {
        String sql = "SELECT id, name, description, is_active, max_participants, created_at, creator_id, subject_id FROM virtual_room WHERE id = ?";
        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public int insert(VirtualRoom room) throws SQLException {
        String sql = "INSERT INTO virtual_room (name, description, is_active, max_participants, created_at, creator_id, subject_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, room.name());
            ps.setString(2, room.description());
            ps.setBoolean(3, room.isActive());
            ps.setInt(4, room.maxParticipants());
            ps.setTimestamp(5, Timestamp.valueOf(room.createdAt()));
            ps.setInt(6, room.creatorId());
            setNullableInt(ps, 7, room.subjectId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    public boolean update(VirtualRoom room) throws SQLException {
        if (room.id() == null) throw new IllegalArgumentException("VirtualRoom id must not be null for update.");
        String sql = "UPDATE virtual_room SET name = ?, description = ?, is_active = ?, max_participants = ?, created_at = ?, creator_id = ?, subject_id = ? WHERE id = ?";
        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, room.name());
            ps.setString(2, room.description());
            ps.setBoolean(3, room.isActive());
            ps.setInt(4, room.maxParticipants());
            ps.setTimestamp(5, Timestamp.valueOf(room.createdAt()));
            ps.setInt(6, room.creatorId());
            setNullableInt(ps, 7, room.subjectId());
            ps.setInt(8, room.id());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM virtual_room WHERE id = ?";
        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Symfony parity: VirtualRoomRepository methods ────────────────────────

    /**
     * Returns all active rooms, optionally filtered by subject.
     * Mirrors: findActiveRooms(?int $subjectId = null): array
     */
    public List<VirtualRoom> findActiveRooms(Integer subjectId) throws SQLException {
        String sql = subjectId != null
                ? "SELECT id, name, description, is_active, max_participants, created_at, creator_id, subject_id " +
                  "FROM virtual_room WHERE is_active = TRUE AND subject_id = ? ORDER BY created_at DESC"
                : "SELECT id, name, description, is_active, max_participants, created_at, creator_id, subject_id " +
                  "FROM virtual_room WHERE is_active = TRUE ORDER BY created_at DESC";

        List<VirtualRoom> rooms = new ArrayList<>();
        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            if (subjectId != null) ps.setInt(1, subjectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rooms.add(mapRow(rs));
            }
        }
        return rooms;
    }

    /**
     * Returns all rooms ordered by creation date descending.
     * Mirrors: findAllWithDetails(): array
     */
    public List<VirtualRoom> findAllWithDetails() throws SQLException {
        String sql = "SELECT id, name, description, is_active, max_participants, created_at, creator_id, subject_id " +
                     "FROM virtual_room ORDER BY created_at DESC";
        List<VirtualRoom> rooms = new ArrayList<>();
        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) rooms.add(mapRow(rs));
        }
        return rooms;
    }

    /**
     * Counts the number of participants in a room.
     * Used to check isFull() before joining.
     */
    public int countParticipants(int roomId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM virtual_room_participants WHERE virtual_room_id = ?";
        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Checks whether a user is already a participant in a room.
     * Mirrors: isParticipant(User $user) on the VirtualRoom entity.
     */
    public boolean isParticipant(int roomId, int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM virtual_room_participants WHERE virtual_room_id = ? AND user_id = ?";
        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    /**
     * Adds a user as a participant in a room (join).
     * Mirrors: addParticipant(User $user) + flush().
     */
    public void joinRoom(int roomId, int userId) throws SQLException {
        if (isParticipant(roomId, userId)) return; // already joined
        String sql = "INSERT INTO virtual_room_participants (virtual_room_id, user_id) VALUES (?, ?)";
        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Removes a user from a room (leave).
     * Mirrors: removeParticipant(User $user) + flush().
     */
    public void leaveRoom(int roomId, int userId) throws SQLException {
        String sql = "DELETE FROM virtual_room_participants WHERE virtual_room_id = ? AND user_id = ?";
        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Closes a room by setting is_active = false.
     * Mirrors: setIsActive(false) when creator leaves.
     */
    public void closeRoom(int roomId) throws SQLException {
        String sql = "UPDATE virtual_room SET is_active = FALSE WHERE id = ?";
        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.executeUpdate();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private VirtualRoom mapRow(ResultSet rs) throws SQLException {
        return new VirtualRoom(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getBoolean("is_active"),
                rs.getInt("max_participants"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                rs.getInt("creator_id"),
                getNullableInt(rs, "subject_id")
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
}
