package tn.esprit.services.guardian;

import tn.esprit.Entity.Guardian.RoomMessage;
import tn.esprit.db.DatabaseConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RoomMessageService {

    public List<RoomMessage> findByRoomId(int roomId) throws SQLException {
        String sql = "SELECT id, content, is_edited, created_at, edited_at, sender_id, virtual_room_id "
            + "FROM chat_message WHERE virtual_room_id = ? ORDER BY created_at ASC, id ASC";
        List<RoomMessage> messages = new ArrayList<>();

        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapRow(rs));
                }
            }
        }

        return messages;
    }

    public int insert(RoomMessage message) throws SQLException {
        String sql = "INSERT INTO chat_message "
            + "(content, is_edited, created_at, edited_at, sender_id, virtual_room_id) "
            + "VALUES (?, ?, ?, ?, ?, ?)";

        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, message.content());
            ps.setBoolean(2, Boolean.TRUE.equals(message.isEdited()));
            ps.setTimestamp(3, Timestamp.valueOf(message.createdAt()));
            setNullableTimestamp(ps, 4, message.editedAt());
            setNullableInt(ps, 5, message.senderId());
            ps.setInt(6, message.virtualRoomId());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }

        return -1;
    }

    private RoomMessage mapRow(ResultSet rs) throws SQLException {
        return new RoomMessage(
                rs.getInt("id"),
                rs.getString("content"),
                rs.getBoolean("is_edited"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("edited_at")),
                getNullableInt(rs, "sender_id"),
                rs.getInt("virtual_room_id")
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
