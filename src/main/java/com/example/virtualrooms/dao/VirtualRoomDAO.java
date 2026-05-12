package com.example.virtualrooms.dao;

import com.example.util.DBConnection;
import com.example.entity.User;
import com.example.entity.VirtualRoom;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VirtualRoomDAO {
    private final String roomTable;
    private final String userTable;
    private final boolean hasSubject;
    private final boolean hasCapacity;
    private final boolean hasStatus;
    private final boolean hasCreator;
    private final boolean hasCreatedBy;
    private final boolean hasRoomType;
    private final boolean hasIsActive;
    private final boolean hasJoinedAt;

    public VirtualRoomDAO() {
        this.roomTable = resolveFirstExistingTable("virtual_room", "virtual_rooms");
        this.userTable = resolveFirstExistingTable("user", "users");
        this.hasSubject = columnExists(roomTable, "subject");
        this.hasCapacity = columnExists(roomTable, "capacity");
        this.hasStatus = columnExists(roomTable, "status");
        this.hasCreator = columnExists(roomTable, "creator_id");
        this.hasCreatedBy = columnExists(roomTable, "created_by_id");
        this.hasRoomType = columnExists(roomTable, "room_type");
        this.hasIsActive = columnExists(roomTable, "is_active");
        this.hasJoinedAt = columnExists("virtual_room_participants", "joined_at");
    }

    public List<VirtualRoom> findAllWithStats(int currentUserId) {
        String creatorExpr = hasCreator ? "vr.creator_id" : (hasCreatedBy ? "vr.created_by_id" : "NULL");
        String subjectExpr = hasSubject ? "vr.subject" : (hasRoomType ? "vr.room_type" : "''");
        String capacityExpr = hasCapacity ? "vr.capacity" : "10";
        String statusExpr = hasStatus ? "vr.status" : (hasIsActive ? "CASE WHEN vr.is_active = 1 THEN 'ACTIVE' ELSE 'INACTIVE' END" : "'ACTIVE'");

        String sql =
                "SELECT vr.id, vr.name, vr.description, " + subjectExpr + " AS subject, " +
                        capacityExpr + " AS capacity, " + statusExpr + " AS status, " +
                        creatorExpr + " AS creator_id, vr.created_at, " +
                        "COALESCE(COUNT(DISTINCT p_all.user_id), 0) AS participant_count, " +
                        "MAX(CASE WHEN p_me.user_id IS NULL THEN 0 ELSE 1 END) AS joined, " +
                        "MAX(u.username) AS creator_name " +
                        "FROM " + roomTable + " vr " +
                        "LEFT JOIN virtual_room_participants p_all ON p_all.virtual_room_id = vr.id AND (p_all.is_active = 1 OR p_all.is_active IS NULL) " +
                        "LEFT JOIN virtual_room_participants p_me ON p_me.virtual_room_id = vr.id AND p_me.user_id = ? AND (p_me.is_active = 1 OR p_me.is_active IS NULL) " +
                        "LEFT JOIN " + userTable + " u ON u.id = " + creatorExpr + " " +
                        "GROUP BY vr.id, vr.name, vr.description, subject, capacity, status, creator_id, vr.created_at " +
                        "ORDER BY vr.created_at DESC";

        List<VirtualRoom> rooms = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rooms.add(mapRoom(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to load rooms: " + e.getMessage(), e);
        }
        return rooms;
    }

    public Optional<VirtualRoom> findById(int roomId, int currentUserId) {
        return findAllWithStats(currentUserId).stream().filter(r -> r.getId() == roomId).findFirst();
    }

    public boolean isUserParticipant(int roomId, int userId) {
        String sql = "SELECT 1 FROM virtual_room_participants WHERE virtual_room_id = ? AND user_id = ? AND (is_active = 1 OR is_active IS NULL)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to verify participant: " + e.getMessage(), e);
        }
    }

    public int countParticipants(int roomId) {
        String sql = "SELECT COUNT(*) FROM virtual_room_participants WHERE virtual_room_id = ? AND (is_active = 1 OR is_active IS NULL)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to count participants: " + e.getMessage(), e);
        }
    }

    public boolean joinRoom(int roomId, int userId) {
        String sql = hasJoinedAt
            ? "INSERT INTO virtual_room_participants (virtual_room_id, user_id, is_active, joined_at) VALUES (?, ?, 1, CURRENT_TIMESTAMP) " +
              "ON DUPLICATE KEY UPDATE is_active = 1, joined_at = CURRENT_TIMESTAMP"
            : "INSERT INTO virtual_room_participants (virtual_room_id, user_id, is_active) VALUES (?, ?, 1) " +
              "ON DUPLICATE KEY UPDATE is_active = 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Unable to join room: " + e.getMessage(), e);
        }
    }

    public boolean leaveRoom(int roomId, int userId) {
        String sql = "DELETE FROM virtual_room_participants WHERE virtual_room_id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Unable to leave room: " + e.getMessage(), e);
        }
    }

    public List<ParticipantInfo> findParticipants(int roomId) {
        String orderBy = hasJoinedAt ? " ORDER BY p.joined_at ASC" : " ORDER BY COALESCE(NULLIF(u.email, ''), u.username) ASC";
        String sql = "SELECT u.id, u.username, u.email FROM virtual_room_participants p " +
                "JOIN " + userTable + " u ON u.id = p.user_id " +
                "WHERE p.virtual_room_id = ? AND (p.is_active = 1 OR p.is_active IS NULL) " +
            orderBy;
        List<ParticipantInfo> participants = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    participants.add(new ParticipantInfo(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("email")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to load participants: " + e.getMessage(), e);
        }
        return participants;
    }

    private VirtualRoom mapRoom(ResultSet rs) throws SQLException {
        VirtualRoom room = new VirtualRoom();
        room.setId(rs.getInt("id"));
        room.setName(rs.getString("name"));
        room.setDescription(rs.getString("description"));
        room.setSubject(rs.getString("subject"));
        int capacity = rs.getInt("capacity");
        room.setCapacity(capacity <= 0 ? 10 : capacity);
        room.setStatus(rs.getString("status"));
        int creatorId = rs.getInt("creator_id");
        room.setCreatorId(rs.wasNull() ? null : creatorId);
        room.setCreatorName(rs.getString("creator_name"));
        Timestamp createdTs = rs.getTimestamp("created_at");
        room.setCreatedAt(createdTs != null ? createdTs.toLocalDateTime() : LocalDateTime.now());
        room.setParticipantCount(rs.getInt("participant_count"));
        room.setCurrentUserJoined(rs.getInt("joined") == 1);
        return room;
    }

    private String resolveFirstExistingTable(String... candidates) {
        try (Connection conn = DBConnection.getConnection()) {
            for (String table : candidates) {
                if (tableExists(conn, table)) return table;
            }
        } catch (SQLException ignored) {}
        return candidates[0];
    }

    private boolean tableExists(Connection conn, String table) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getTables(conn.getCatalog(), null, table, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private boolean columnExists(String table, String column) {
        try (Connection conn = DBConnection.getConnection();
             ResultSet rs = conn.getMetaData().getColumns(conn.getCatalog(), null, table, column)) {
            return rs.next();
        } catch (SQLException ignored) {
            return false;
        }
    }

    public static class ParticipantInfo {
        private final int userId;
        private final String username;
        private final String email;

        public ParticipantInfo(int userId, String username, String email) {
            this.userId = userId;
            this.username = username;
            this.email = email;
        }

        public int getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public String getEmail() {
            return email;
        }

        public String getDisplayName() {
            if (email != null && !email.isBlank()) {
                return email;
            }
            return username != null && !username.isBlank() ? username : "Unknown";
        }
    }
}
