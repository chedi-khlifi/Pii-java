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

    public List<VirtualRoom> findAll() throws SQLException {
        String sql = "SELECT id, name, description, is_active, max_participants, created_at, creator_id, subject_id FROM virtual_room";
        List<VirtualRoom> rooms = new ArrayList<>();

        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rooms.add(mapRow(rs));
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
                if (rs.next()) {
                    return mapRow(rs);
                }
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
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }

        return -1;
    }

    public boolean update(VirtualRoom room) throws SQLException {
        if (room.id() == null) {
            throw new IllegalArgumentException("VirtualRoom id must not be null for update.");
        }

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
        if (value == null) {
            ps.setNull(index, java.sql.Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }
}
